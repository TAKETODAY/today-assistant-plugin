/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.assistant.model.config.autoconfigure;

import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.util.Function;
import com.intellij.util.SmartList;
import com.intellij.util.ThrowableConsumer;
import com.intellij.util.containers.FactoryMap;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.DataInputOutputUtil;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.IOUtil;
import com.intellij.util.io.KeyDescriptor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.lang.Nullable;
import kotlin.collections.CollectionsKt;
import kotlin.collections.MapsKt;
import kotlin.text.StringsKt;

public final class AutoConfigureMetadataIndex
        extends FileBasedIndexExtension<String, AutoConfigureMetadataIndex.AutoConfigureMetadata> {
  private static final ID<String, AutoConfigureMetadata> NAME = ID.create("infra.autoConfigureMetadataIndex");

  public static final String INFRA_AUTOCONFIGURE_METADATA_PROPERTIES_FILENAME = "infra-autoconfigure-metadata.properties";

  @Nullable
  public static AutoConfigureMetadata findMetadata(PsiClass autoConfigClass) {
    String fqn = autoConfigClass.getQualifiedName();
    if (fqn != null) {
      List<AutoConfigureMetadata> values = FileBasedIndex.getInstance().getValues(NAME, fqn, autoConfigClass.getResolveScope());
      return CollectionsKt.firstOrNull(values);
    }
    return null;
  }

  public ID<String, AutoConfigureMetadata> getName() {
    return NAME;
  }

  public DataIndexer getIndexer() {
    return new DataIndexer<String, AutoConfigureMetadata, FileContent>() {

      public Map<String, AutoConfigureMetadata> map(FileContent inputData) {
        PsiFile psiFile = inputData.getPsiFile();
        if (!(psiFile instanceof PropertiesFile)) {
          psiFile = null;
        }
        PropertiesFile file = (PropertiesFile) psiFile;
        if (file != null) {
          Map<String, AutoConfigureMetadata> result = FactoryMap.create(new Function<String, AutoConfigureMetadata>() {
            @Override
            public AutoConfigureMetadata fun(String s) {
              return new AutoConfigureMetadata(null, null, null, null, 15);
            }
          });

          for (IProperty property : file.getProperties()) {
            String key = property.getKey();
            if (key != null) {
              String configClassFqn = StringsKt.substringBeforeLast(key, '.', null);
              if (configClassFqn.length() != 0) {
                AutoConfigureMetadata autoConfigureMetadata = result.get(configClassFqn);
                if (StringsKt.endsWith(key, ".ConditionalOnClass", false)) {
                  autoConfigureMetadata.getConditionalOnClass().addAll(getValues(property));
                }
                else if (StringsKt.endsWith(key, ".AutoConfigureAfter", false)) {
                  autoConfigureMetadata.getAutoConfigureAfter().addAll(getValues(property));
                }
                else if (StringsKt.endsWith(key, ".AutoConfigureBefore", false)) {
                  autoConfigureMetadata.getAutoConfigureBefore().addAll(getValues(property));
                }
                else if (StringsKt.endsWith(key, ".AutoConfigureOrder", false)) {
                  String value = property.getValue();
                  autoConfigureMetadata.setAutoConfigureOrder(value != null ? StringsKt.toIntOrNull(value) : null);
                }
              }
            }
          }
          return result;
        }
        return MapsKt.emptyMap();
      }

      private List<String> getValues(IProperty property) {
        String value = property.getValue();
        if (value == null) {
          value = "";
        }
        return StringsKt.split(value, new char[] { ',' }, false, 0);
      }
    };
  }

  public KeyDescriptor<String> getKeyDescriptor() {
    return EnumeratorStringDescriptor.INSTANCE;
  }

  public DataExternalizer<AutoConfigureMetadata> getValueExternalizer() {
    return new DataExternalizer<>() {
      @Override
      public void save(DataOutput out, AutoConfigureMetadata value) throws IOException {
        DataInputOutputUtil.writeNullable(out, value.getAutoConfigureOrder(), new ThrowableConsumer<Integer, IOException>() {
          @Override
          public void consume(Integer integer) throws IOException {
            DataInputOutputUtil.writeINT(out, integer);
          }
        });
        IOUtil.writeStringList(out, value.getConditionalOnClass());
        IOUtil.writeStringList(out, value.getAutoConfigureBefore());
        IOUtil.writeStringList(out, value.getAutoConfigureAfter());
      }

      @Override
      public AutoConfigureMetadata read(DataInput dataInput) throws IOException {
        AutoConfigureMetadata metadata = new AutoConfigureMetadata(null, null, null, null, 15);
        metadata.setAutoConfigureOrder(DataInputOutputUtil.readNullable(dataInput, new ThrowableComputable<Integer, IOException>() {
          @Override
          public Integer compute() throws IOException {
            return DataInputOutputUtil.readINT(dataInput);
          }
        }));
        List<String> conditionalOnClass = metadata.getConditionalOnClass();
        List<String> readStringList = IOUtil.readStringList(dataInput);
        conditionalOnClass.addAll(readStringList);
        List<String> autoConfigureBefore = metadata.getAutoConfigureBefore();
        List<String> readStringList2 = IOUtil.readStringList(dataInput);
        autoConfigureBefore.addAll(readStringList2);
        List<String> autoConfigureAfter = metadata.getAutoConfigureAfter();
        List<String> readStringList3 = IOUtil.readStringList(dataInput);
        autoConfigureAfter.addAll(readStringList3);
        return metadata;
      }
    };
  }

  public int getVersion() {
    return 2;
  }

  public FileBasedIndex.InputFilter getInputFilter() {
    return new FileBasedIndex.InputFilter() {
      @Override
      public boolean acceptInput(VirtualFile file) {
        return Comparing.equal(file.getNameSequence(), INFRA_AUTOCONFIGURE_METADATA_PROPERTIES_FILENAME);
      }
    };
  }

  public boolean dependsOnFileContent() {
    return true;
  }

  public static final class AutoConfigureMetadata {
    @Nullable
    private Integer autoConfigureOrder;

    private final List<String> conditionalOnClass;

    private final List<String> autoConfigureAfter;

    private final List<String> autoConfigureBefore;

    public AutoConfigureMetadata() {
      this(null, null, null, null, 15);
    }

    @Nullable
    public Integer component1() {
      return this.autoConfigureOrder;
    }

    public List<String> component2() {
      return this.conditionalOnClass;
    }

    public List<String> component3() {
      return this.autoConfigureAfter;
    }

    public List<String> component4() {
      return this.autoConfigureBefore;
    }

    public AutoConfigureMetadata copy(@Nullable Integer autoConfigureOrder, List<String> list, List<String> list2, List<String> list3) {
      return new AutoConfigureMetadata(autoConfigureOrder, list, list2, list3);
    }

    public static AutoConfigureMetadata copy$default(AutoConfigureMetadata autoConfigureMetadata, Integer num, List list, List list2, List list3, int i, Object obj) {
      if ((i & 1) != 0) {
        num = autoConfigureMetadata.autoConfigureOrder;
      }
      if ((i & 2) != 0) {
        list = autoConfigureMetadata.conditionalOnClass;
      }
      if ((i & 4) != 0) {
        list2 = autoConfigureMetadata.autoConfigureAfter;
      }
      if ((i & 8) != 0) {
        list3 = autoConfigureMetadata.autoConfigureBefore;
      }
      return autoConfigureMetadata.copy(num, list, list2, list3);
    }

    public String toString() {
      return "AutoConfigureMetadata(autoConfigureOrder=" + this.autoConfigureOrder + ", conditionalOnClass=" + this.conditionalOnClass + ", autoConfigureAfter=" + this.autoConfigureAfter + ", autoConfigureBefore=" + this.autoConfigureBefore + ")";
    }

    public int hashCode() {
      Integer num = this.autoConfigureOrder;
      int hashCode = (num != null ? num.hashCode() : 0) * 31;
      List<String> list = this.conditionalOnClass;
      int hashCode2 = (hashCode + (list != null ? list.hashCode() : 0)) * 31;
      List<String> list2 = this.autoConfigureAfter;
      int hashCode3 = (hashCode2 + (list2 != null ? list2.hashCode() : 0)) * 31;
      List<String> list3 = this.autoConfigureBefore;
      return hashCode3 + (list3 != null ? list3.hashCode() : 0);
    }

    public boolean equals(@Nullable Object obj) {
      if (this != obj) {
        if (!(obj instanceof AutoConfigureMetadata autoConfigureMetadata)) {
          return false;
        }
        return Objects.equals(this.autoConfigureOrder, autoConfigureMetadata.autoConfigureOrder)
                && Objects.equals(this.conditionalOnClass, autoConfigureMetadata.conditionalOnClass)
                && Objects.equals(this.autoConfigureAfter, autoConfigureMetadata.autoConfigureAfter)
                && Objects.equals(this.autoConfigureBefore, autoConfigureMetadata.autoConfigureBefore);
      }
      return true;
    }

    @Nullable
    public Integer getAutoConfigureOrder() {
      return this.autoConfigureOrder;
    }

    public void setAutoConfigureOrder(@Nullable Integer num) {
      this.autoConfigureOrder = num;
    }

    public AutoConfigureMetadata(@Nullable Integer autoConfigureOrder, List<String> list, List<String> list2, List<String> list3) {
      this.autoConfigureOrder = autoConfigureOrder;
      this.conditionalOnClass = list;
      this.autoConfigureAfter = list2;
      this.autoConfigureBefore = list3;
    }

    public AutoConfigureMetadata(Integer num, List list, List list2, List list3, int i) {
      this((i & 1) != 0 ? null : num, (i & 2) != 0 ? new SmartList() : list, (i & 4) != 0 ? new SmartList() : list2, (i & 8) != 0 ? new SmartList() : list3);
    }

    public List<String> getConditionalOnClass() {
      return this.conditionalOnClass;
    }

    public List<String> getAutoConfigureAfter() {
      return this.autoConfigureAfter;
    }

    public List<String> getAutoConfigureBefore() {
      return this.autoConfigureBefore;
    }
  }

}
