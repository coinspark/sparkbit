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
package org.sparkbit.jsonrpc;

import java.io.IOException;
import java.util.Date;
 
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
public class DatePrintServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
 
	@Override
	protected void doGet(final HttpServletRequest req,
			final HttpServletResponse res) throws ServletException, IOException {
		res.getWriter()
				.append(String.format("It's %s now\n\n\n\nThis is just a test!",
						new Date()));
	}
}

