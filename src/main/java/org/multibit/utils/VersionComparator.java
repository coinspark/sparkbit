/* 
 * SparkBit
 *
 * Copyright 2011-2014 multibit.org
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
package org.multibit.utils;

import java.util.Comparator;

/**
 * Class to compare MultiBit version numbers. Version numbers are ordered as
 * follows 0.4.0 0.4.1-SNAPSHOT 0.4.1alpha1 0.4.1beta1 0.4.1beta2 0.4.1rc1 0.4.1
 */
public class VersionComparator implements Comparator<String> {

    private static int MAXIMIMUM_NUMBER_PER_CATEGORY = 1000;
    private static String ALPHA_STRING = "alpha";
    private static String BETA_STRING = "beta";
    private static String RELEASE_CANDIDATE_STRING = "rc";
    private static String SNAPSHOT_STRING = "-snapshot";

    @Override
    public int compare(String first, String second) {
        Long firstOrdinal = calculateOrdinal(first);
        Long secondOrdinal = calculateOrdinal(second);
               
        return firstOrdinal.compareTo(secondOrdinal);
    }

    Long calculateOrdinal(String version) throws NumberFormatException {
        long ordinal = 0;

        version = version.toLowerCase();

        int isAlpha = version.indexOf(ALPHA_STRING) > -1 ? 1 : 0;
        int isBeta = version.indexOf(BETA_STRING) > -1 ? 1 : 0;
        int isReleaseCandidate = version.indexOf(RELEASE_CANDIDATE_STRING) > -1 ? 1 : 0;
        int isSnapshot = version.indexOf(SNAPSHOT_STRING) > -1 ? 1 : 0;

        version = version.replaceFirst(ALPHA_STRING, ".");
        version = version.replaceFirst(BETA_STRING, ".");
        version = version.replaceFirst(RELEASE_CANDIDATE_STRING, ".");
        version = version.replaceFirst(SNAPSHOT_STRING, ".");

        String[] tokens = version.split("\\.");

        if (tokens.length > 0) {
            ordinal = ordinal + (parseInt(tokens[0]) * MAXIMIMUM_NUMBER_PER_CATEGORY * MAXIMIMUM_NUMBER_PER_CATEGORY * MAXIMIMUM_NUMBER_PER_CATEGORY);
        }
        if (tokens.length > 1) {
            ordinal = ordinal + (parseInt(tokens[1]) * MAXIMIMUM_NUMBER_PER_CATEGORY  * MAXIMIMUM_NUMBER_PER_CATEGORY);
        }
        if (tokens.length > 2) {
            ordinal = ordinal + (parseInt(tokens[2]) * MAXIMIMUM_NUMBER_PER_CATEGORY);
        }
        if (tokens.length > 3) {
            ordinal = ordinal + parseInt(tokens[3]);
        }
        ordinal =  ordinal -  MAXIMIMUM_NUMBER_PER_CATEGORY / 5 * (isSnapshot * 4 + isAlpha * 3 + isBeta * 2 + isReleaseCandidate);

        return ordinal;
    }
    
    private int parseInt(String intString) {
        int toReturn = 0;
        try {
            toReturn = Integer.parseInt(intString);
        } catch (NumberFormatException nfe) {
            // Do nothing.
        }
        return toReturn;
    }
}
