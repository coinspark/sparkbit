/*
 * SparkBit
 *
 * Copyright 2014 Coin Sciences Ltd
 *
 * Licensed under the MIT license (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://opensource.org/licenses/mit-license.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sparkbit.utils;
import java.util.Arrays;
/**
    Make sure bad filename characters are not used by wallet name.
    http://en.wikipedia.org/wiki/Filename#Comparison%5Fof%5Ffile%5Fname%5Flimitations
    http://en.wikipedia.org/wiki/Filename#Reserved%5Fcharacters%5Fand%5Fwords
    
    This unicode-safe code is from:
    http://stackoverflow.com/questions/1155107/is-there-a-cross-platform-java-method-to-remove-filename-special-chars
    */
public class FileNameCleaner {
  final static int[] illegalChars = {34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47};

  static {
    Arrays.sort(illegalChars);
  }

  public static String cleanFileName(String badFileName) {
    StringBuilder cleanName = new StringBuilder();
    int len = badFileName.codePointCount(0, badFileName.length());
    for (int i=0; i<len; i++) {
      int c = badFileName.codePointAt(i);
      if (Arrays.binarySearch(illegalChars, c) < 0) {
        cleanName.appendCodePoint(c);
      }
    }
    return cleanName.toString();
  }
}
