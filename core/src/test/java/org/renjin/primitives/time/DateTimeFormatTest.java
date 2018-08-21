/*
 * Renjin : JVM-based interpreter for the R language for the statistical analysis
 * Copyright © 2010-2018 BeDataDriven Groep B.V. and contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.gnu.org/licenses/gpl-2.0.txt
 */
package org.renjin.primitives.time;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class DateTimeFormatTest {

  @Test
  public void formatBuilder() {
    verifyFormat("2009-07-01 00:00:00", "%Y-%m-%d %H:%M:%OS", new DateTime(2009,7,1,0,0,0));
  }

  @Test
  public void newlines() {
    verifyFormat("Aug 21\n2018", "%b %d%n%Y", new DateTime(2018, 8, 21, 0, 0, 0));
  }


  private void verifyFormat(String x, String format, DateTime dateTime) {
    DateTimeFormatter formatter = DateTimeFormat.forPattern(format);
    assertThat(formatter.print(dateTime), equalTo(x));
    assertThat(formatter.parseDateTime(x), equalTo(dateTime));
  }
  
}
