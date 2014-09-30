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
package org.multibit.viewsystem.swing.view.components;

import java.awt.Dimension;

import javax.swing.Action;

import org.multibit.controller.Controller;

public class HelpButton extends MultiBitButton {

    private static final long serialVersionUID = 6708096174704292284L;

    public HelpButton(Action action, Controller controller) {
        this(action, controller, false);
    }

    public HelpButton(Action action, Controller controller, boolean paintBorder) {
        super(action, controller);

        setBorderPainted(paintBorder);
        setContentAreaFilled(paintBorder);
        setFocusPainted(false);

        if (getIcon() != null && (getText() == null || "".equals(getText()))) {
            int width = getIcon().getIconWidth();
            int height = getIcon().getIconHeight();
            setPreferredSize(new Dimension(width, height));
        }

    }
}
