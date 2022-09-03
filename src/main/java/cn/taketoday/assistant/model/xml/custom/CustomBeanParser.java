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

package cn.taketoday.assistant.model.xml.custom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class CustomBeanParser {
  public static final String MAGIC = "\n\n##$%^$&%@^#%$#%^&$^&%*&^(*(^&*(&^*&%*&%&*^\n";

  public static void main(String[] args) {
    Logger.getLogger("").setLevel(Level.FINE);
    Logger.getLogger("").addHandler(new Handler() {
      @Override
      public void publish(LogRecord record) {
        Throwable throwable = record.getThrown();
        if (throwable != null) {
          CustomBeanParser.printException(throwable);
        }
      }

      @Override
      public void flush() {
      }

      @Override
      public void close() throws SecurityException {
      }
    });
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    try {
      int timeout = Integer.parseInt(input.readLine());
      while ("input".equals(input.readLine())) {
        String tagText = decode(input.readLine());
        CustomBeanParserUtil.parseCustomBean(tagText, timeout);
        System.out.flush();
      }
    }
    catch (Throwable e) {
      try {
        printException(e);
        try {
          input.close();
        }
        catch (IOException e2) {
        }
        System.exit(0);
      }
      finally {
        try {
          input.close();
        }
        catch (IOException e3) {
        }
        System.exit(0);
      }
    }
  }

  public static void printException(Throwable e) {
    System.out.print("exception\n");
    StringWriter writer = new StringWriter();
    e.printStackTrace(new PrintWriter(writer));
    System.out.print(encode(writer.toString()) + MAGIC);
    System.out.flush();
  }

  public static String decode(String s1) {
    StringBuilder buffer = new StringBuilder();
    int length = s1.length();
    boolean escaped = false;
    int idx = 0;
    while (idx < length) {
      char ch = s1.charAt(idx);
      if (!escaped) {
        if (ch == '\\') {
          escaped = true;
        }
        else {
          buffer.append(ch);
        }
      }
      else {
        switch (ch) {
          case '\"':
            buffer.append('\"');
            break;
          case '\'':
            buffer.append('\'');
            break;
          case '\\':
            buffer.append('\\');
            break;
          case 'b':
            buffer.append('\b');
            break;
          case 'f':
            buffer.append('\f');
            break;
          case 'n':
            buffer.append('\n');
            break;
          case 'r':
            buffer.append('\r');
            break;
          case 't':
            buffer.append('\t');
            break;
          case 'u':
            if (idx + 4 < length) {
              try {
                int code = Integer.valueOf(s1.substring(idx + 1, idx + 5), 16);
                idx += 4;
                buffer.append((char) code);
                break;
              }
              catch (NumberFormatException e) {
                buffer.append("\\u");
                break;
              }
            }
            else {
              buffer.append("\\u");
              break;
            }
          default:
            buffer.append(ch);
            break;
        }
        escaped = false;
      }
      idx++;
    }
    if (escaped) {
      buffer.append('\\');
    }
    return buffer.toString();
  }

  public static String encode(String s1) {
    StringBuilder buffer = new StringBuilder();
    for (int idx = 0; idx < s1.length(); idx++) {
      char ch = s1.charAt(idx);
      switch (ch) {
        case '\b':
          buffer.append("\\b");
          break;
        case '\t':
          buffer.append("\\t");
          break;
        case '\n':
          buffer.append("\\n");
          break;
        case '\f':
          buffer.append("\\f");
          break;
        case '\r':
          buffer.append("\\r");
          break;
        case '\"':
          buffer.append("\\\"");
          break;
        case '\\':
          buffer.append("\\\\");
          break;
        default:
          if (Character.isISOControl(ch)) {
            String hexCode = Integer.toHexString(ch).toUpperCase(Locale.ENGLISH);
            buffer.append("\\u");
            int paddingCount = 4 - hexCode.length();
            while (true) {
              int i = paddingCount;
              paddingCount--;
              if (i > 0) {
                buffer.append(0);
              }
              else {
                buffer.append(hexCode);
                break;
              }
            }
          }
          else {
            buffer.append(ch);
            break;
          }
      }
    }
    return buffer.toString();
  }
}
