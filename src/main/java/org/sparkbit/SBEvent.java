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
package org.sparkbit;

/**
 * Same as CSEvent.  We can push these events onto the CSEventBus.
 */
public class SBEvent {
    private SBEventType type;
    private Object info;

    public SBEvent(SBEventType type) {
	this.type = type;
	this.info = null;
    }

    public SBEvent(SBEventType type, Object info) {
	this.type = type;
	this.info = info;
    }

    public SBEventType getType() {
	return type;
    }

    public Object getInfo() {
	return info;
    }
}
