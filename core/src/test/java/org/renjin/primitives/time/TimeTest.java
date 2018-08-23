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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.renjin.EvalTestCase;
import org.renjin.sexp.IntVector;
import org.renjin.sexp.Null;
import org.renjin.sexp.SEXP;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests the POSIXlt/POSIXct/Date parsing, conversion, and formatting routines.
 * 
 * <p>This test is parameterized with several timezones to ensure that the test results 
 * are valid regardless of local timezone</p>
 */
@RunWith(Parameterized.class)
public class TimeTest extends EvalTestCase {

  @Before
  public void specifyLocale() {
    // Many aspects of time formatting and parsing are locale-specific
    // so we need to fix the locale to get reproducible results
    // TODO: Add tests to ensure Renjin works correctly on other locales other than English
    Locale.setDefault(Locale.ENGLISH);
  }
  

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"Pacific/Pago_Pago"},    // -11:00 / -11:00
        {"Pacific/Honolulu"},     // -10:00
        {"America/Los_Angeles"},  // -08:00 / -07;00
        {"Europe/London"},        //  00:00 / +01:00
        {"UTC"},                  //  00:00
        {"Europe/Amsterdam"},     // +01:00 / -02:00
        {"Africa/Lubumbashi"},    // +02:00
        {"Asia/Kabul"},           // +04:30
        {"Asia/Hong_Kong"},       // +08:00
        {"Pacific/Fiji"},         // +12:00 / +13:00
        {"Pacific/Kiritimati"}    // +14:00
    });
  }
  
  public TimeTest(String defaultTimeZoneId) {
    TimeZone.setDefault(TimeZone.getTimeZone(defaultTimeZoneId));
  }

  @Test
  public void strptime() {
    
    eval("t <- .Internal(strptime('2009-07-01 18:14:05', '%Y-%m-%d %H:%M:%OS', 'Europe/Amsterdam'))");
    
    assertThat(eval("t$sec"), elementsIdenticalTo(c_i(5)));
    assertThat(eval("t$min"), elementsIdenticalTo(c_i(14)));
    assertThat(eval("t$hour"), elementsIdenticalTo(c_i(18)));
    assertThat(eval("t$mday"), elementsIdenticalTo(c_i(1)));
    assertThat(eval("t$mon"), elementsIdenticalTo(c_i(6)));
    assertThat(eval("t$year"), elementsIdenticalTo(c_i(109)));
    assertThat(eval("t$wday"), elementsIdenticalTo(c_i(3)));
    assertThat(eval("t$yday"), elementsIdenticalTo(c_i(181)));
    
    
    // Verify that the daylight savings flag is set 
    // Because we specified timezone above, this should not be dependent on local settings
    assertThat(eval("t$isdst"), elementsIdenticalTo(c_i(1)));
  }
  
  @Test
  public void asDateWithFormat() {
    eval(" thisDate<- '2016-06-1'  ");
    eval(" thisDate2 <- as.Date(thisDate,'%Y-%m-%d')");
    
    assertThat(eval("thisDate2"), elementsIdenticalTo(c(16953)));
  }
  
  @Test
  public void strptimeDst() {
    // 2015
    // NL: DST (CET) ended on 25 Oct
    // US: DST (EDT) ended on 2 Nov


    assertThat(eval("as.POSIXlt('2015-10-30', tz='Europe/Amsterdam')$isdst"), elementsIdenticalTo(c_i(0)));
    assertThat(eval("as.POSIXlt('2015-10-30', tz='America/New_York')$isdst"), elementsIdenticalTo(c_i(1)));


    assertThat(eval("as.POSIXlt('2015-12-30', tz='Europe/Amsterdam')$isdst"), elementsIdenticalTo(c_i(0)));
    assertThat(eval("as.POSIXlt('2015-12-30', tz='America/New_York')$isdst"), elementsIdenticalTo(c_i(0)));
  }
  
  
  @Test
  public void strptimeWithOffset() {


    // while the string above will be unambiguously parsed thanks to the timezone
    // offset, it will be essentially _converted_ to a POSIXlt object in the default 
    // timezone, making the output to R dependent on the current timezone in which the test is run
    // So for this test only, set the "default" time zone
    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Amsterdam"));

    eval("t <- strptime('24/Aug/2014:17:57:26 +0200', '%d/%b/%Y:%H:%M:%S %z')");

    
    assertThat(eval("t$sec"), elementsIdenticalTo(c_i(26)));
    assertThat(eval("t$min"), elementsIdenticalTo(c_i(57)));
    assertThat(eval("t$hour"), elementsIdenticalTo(c_i(17)));
    assertThat(eval("t$mday"), elementsIdenticalTo(c_i(24)));
    assertThat(eval("t$mon"), elementsIdenticalTo(c_i(7)));
    assertThat(eval("t$year"), elementsIdenticalTo(c_i(114)));
    assertThat(eval("t$wday"), elementsIdenticalTo(c_i(0)));
    assertThat(eval("t$yday"), elementsIdenticalTo(c_i(235)));
    assertThat(eval("t$isdst"), elementsIdenticalTo(c_i(1)));
    assertThat(eval("t$gmtoff"), elementsIdenticalTo(c_i(7200)));
    assertThat(eval("attr(t, 'tzone')"), identicalTo((SEXP)Null.INSTANCE));
  }


  @Test
  public void strptimeWithOffsetWithTzParam() {
    eval("t <- strptime('24/Aug/2014:17:57:26 +0200', '%d/%b/%Y:%H:%M:%S %z', tz = 'Pacific/Honolulu')");
    assertThat(eval("t$sec"), elementsIdenticalTo(c_i(26)));
    assertThat(eval("t$min"), elementsIdenticalTo(c_i(57)));
    assertThat(eval("t$hour"), elementsIdenticalTo(c_i(5)));
    assertThat(eval("t$mday"), elementsIdenticalTo(c_i(24)));
    assertThat(eval("t$mon"), elementsIdenticalTo(c_i(7)));
    assertThat(eval("t$year"), elementsIdenticalTo(c_i(114)));
    assertThat(eval("t$wday"), elementsIdenticalTo(c_i(0)));
    assertThat(eval("t$yday"), elementsIdenticalTo(c_i(235)));
    assertThat(eval("t$isdst"), elementsIdenticalTo(c_i(0)));
    assertThat(eval("attr(t, 'tzone')"), elementsIdenticalTo(c( "Pacific/Honolulu")));

    // GNU R 3.2.2 says this should be 7200, which corresponds to the offset in the parsed string,
    // but NOT to the actual timezone of the other values.

    // Waiting for response from https://bugs.r-project.org/bugzilla/show_bug.cgi?id=16621

    assertThat(eval("t$gmtoff"), elementsIdenticalTo(c_i(7200)));

  }
  
  @Test
  public void seqByMonth() {
    assertThat(eval("s <- seq(from=as.Date('2015-01-01'), to = as.Date('2015-04-01'), by = 'month')"),
        elementsIdenticalTo(c(16436, 16467, 16495, 16526)));
  }
  
  @Test
  public void seqByDay() {
    assertThat(eval("seq(from= as.Date('2005-01-01'), to= as.Date('2005-01-05'), by= 'day')"),
        elementsIdenticalTo(c(12784, 12785, 12786, 12787, 12788)));
  }
  
  @Test
  public void cutByWeek() {
    eval("everyday = seq(from= as.Date('2005-1-1'), to= as.Date('2005-1-14'), by= 'day')");
    eval("res<-cut(everyday, breaks= 'week')");
    assertThat(eval("res"), elementsIdenticalTo(c_i( 1, 1, 2, 2, 2, 2 ,2, 2, 2, 3, 3, 3, 3, 3)));
    assertThat(eval("levels(res)"), elementsIdenticalTo(c("2004-12-27", "2005-01-03", "2005-01-10")));
  }
  
  @Test
  public void timeZones() {
    eval("t <- .Internal(strptime('2011-11-06 09:27', '%Y-%m-%d %H:%M', tz='HST'))");
    assertThat(eval("t$hour"), elementsIdenticalTo(c_i(9)));
    assertThat(eval("attr(t, 'tzone')"), elementsIdenticalTo(c("HST")));
    
    // _t_ now contains a time value expressed as calander date + time offset + time zone
    // as.POSIXct must convert this value to milliseconds since 1970-01-01 UTC.
    assertThat(eval(".Internal(as.POSIXct(t, 'HST'))"), elementsIdenticalTo(c(1320607620d)));
  }
  
  @Test
  public void posixLtWithNegativeFields() {
    eval("lt <-  list(sec = 0L, min = 0L, hour = 0L, mday = -4L, mon = 0L, year = 105L, wday = 6L, yday = 0L, isdst = -1L)");
    eval("class(lt) <- c('POSIXlt', 'POSIXt')");
    
    assertThat(eval("as.character(lt)"), elementsIdenticalTo(c("2004-12-27")));
  }
  
  @Test
  public void asPosixLt() throws IOException {
    topLevelContext.init();
    eval("ct <- as.POSIXct('2009-07-01 00:00:00', tz = 'UTC')");
    assertThat(eval("ct"), elementsIdenticalTo(c(1246406400d)));
    assertThat(eval("attr(ct, 'tzone')"), elementsIdenticalTo(c("UTC")));
    
    eval("lt <- as.POSIXlt(ct)");
    assertThat(eval("lt$sec"), elementsIdenticalTo(c_i(0)));
    assertThat(eval("lt$min"), elementsIdenticalTo(c_i(0)));
    assertThat(eval("lt$hour"), elementsIdenticalTo(c_i(0)));
    assertThat(eval("lt$mday"), elementsIdenticalTo(c_i(1)));
    assertThat(eval("lt$mon"), elementsIdenticalTo(c_i(7-1)));
    assertThat(eval("lt$year"), elementsIdenticalTo(c_i(2009-1900)));
    assertThat(eval("lt$wday"), elementsIdenticalTo(c_i(3)));
    assertThat(eval("lt$yday"), elementsIdenticalTo(c_i(181)));
    assertThat(eval("attr(lt, 'tzone')"), elementsIdenticalTo(c("UTC")));
    
    assertThat(eval("format(ct)"), elementsIdenticalTo(c("2009-07-01")));
    assertThat(eval("names(format(ct))"), identicalTo(NULL));
  }

  @Test
  public void asPosixLtFromInt() {

    eval("ct <- structure(1246406400L, class=c(\"POSIXct\", \"POSIXt\"), tzone = 'UTC')");

    eval("lt <- as.POSIXlt(ct)");
    assertThat(eval("lt$sec"), elementsIdenticalTo(c_i(0)));
    assertThat(eval("lt$min"), elementsIdenticalTo(c_i(0)));
    assertThat(eval("lt$hour"), elementsIdenticalTo(c_i(0)));
    assertThat(eval("lt$mday"), elementsIdenticalTo(c_i(1)));
    assertThat(eval("lt$mon"), elementsIdenticalTo(c_i(7-1)));
    assertThat(eval("lt$year"), elementsIdenticalTo(c_i(2009-1900)));
    assertThat(eval("lt$wday"), elementsIdenticalTo(c_i(3)));
    assertThat(eval("lt$yday"), elementsIdenticalTo(c_i(181)));
    assertThat(eval("attr(lt, 'tzone')"), elementsIdenticalTo(c("UTC")));

    assertThat(eval("format(ct)"), elementsIdenticalTo(c("2009-07-01")));
    assertThat(eval("names(format(ct))"), identicalTo(NULL));
  }

  
  @Test
  public void dateAsPosixLt() {
    eval("d <- as.Date(c('2015-02-15', '2015-04-29'))");
    eval("lt <- as.POSIXlt(d)");
    assertThat(eval("class(lt)"), elementsIdenticalTo(c("POSIXlt", "POSIXt")));
    assertThat(eval("names(unclass(lt))"), 
        elementsIdenticalTo(c("sec", "min", "hour", "mday", "mon", "year", "wday", "yday", "isdst", "gmtoff")));

    assertThat(eval("lt$sec"), elementsIdenticalTo(c_i(0, 0)));
    assertThat(eval("lt$min"), elementsIdenticalTo(c_i(0, 0)));
    assertThat(eval("lt$hour"), elementsIdenticalTo(c_i(0, 0)));
    assertThat(eval("lt$mday"), elementsIdenticalTo(c_i(15, 29)));
    assertThat(eval("lt$mon"), elementsIdenticalTo(c_i(1, 3)));
    assertThat(eval("lt$year"), elementsIdenticalTo(c_i(115, 115)));
    assertThat(eval("lt$wday"), elementsIdenticalTo(c_i(0, 3)));
    assertThat(eval("lt$yday"), elementsIdenticalTo(c_i(45, 118)));
  }
  
  @Test
  public void naPosixLtToDate() {
    eval("lt <- as.POSIXlt(strptime(c('2014-01-01','xxx'), format='%Y-%m-%d'))");

    assertThat(eval("lt$year"), elementsIdenticalTo(c_i(114, IntVector.NA)));

    eval("d <- as.Date(lt)");
    eval("print(d)");
    assertThat(eval("format(d)"), elementsIdenticalTo(c("2014-01-01", null)));
    assertThat(eval("is.na(d)"), elementsIdenticalTo(c(false, true)));
    assertThat(eval("is.na(d[2])"), elementsIdenticalTo(c(true)));
  }

  @Ignore
  public void printTime() throws IOException {
    topLevelContext.init();

    Locale.setDefault(Locale.forLanguageTag("nl"));
    
    StringWriter stringWriter = new StringWriter();
    topLevelContext.getSession().setStdOut(new PrintWriter(stringWriter));

    eval("print(as.POSIXct('2009-07-01 00:00:00', tz = 'Pacific/Honolulu'))");

    assertThat(stringWriter.toString(), equalTo("[1] \"2009-07-01 HST\"\n"));
  }

  @Test
  public void strpTimeSeconds() {

    eval("tt <-  strptime('1978-02-16 12:30:15', format='%Y-%m-%d %H:%M:%S', tz='GMT')");
    assertThat(eval("tt$sec"), elementsIdenticalTo(c_i(15)));
    assertThat(eval("tt$min"), elementsIdenticalTo(c_i(30)));
    assertThat(eval("tt$hour"), elementsIdenticalTo(c_i(12)));
    assertThat(eval("tt$mday"), elementsIdenticalTo(c_i(16)));
    assertThat(eval("tt$mon"), elementsIdenticalTo(c_i(1)));
    assertThat(eval("tt$year"), elementsIdenticalTo(c_i(78)));
    assertThat(eval("tt$wday"), elementsIdenticalTo(c_i(4)));
    assertThat(eval("tt$yday"), elementsIdenticalTo(c_i(46)));
  }
  
  @Test
  public void timeZoneProvided() {

    
    eval("d <- as.Date(as.POSIXct('2000-01-01 00:00:00 GMT', tz = 'GMT'))");
    assertThat(eval("d"), elementsIdenticalTo(c(10957)));
  }

  @Test
  public void strptimeExtra() {
    eval("t <- .Internal(strptime('2000-01-01 00:00:00 GMT', '%Y-%m-%d %H:%M:%OS', ''))");
    assertThat(eval("t$year"), elementsIdenticalTo(c_i(100)));
  }
    
  @Test
  public void strptimeBadInput() {
    eval("t <- .Internal(strptime('FOOOO!!!', '%Y-%m-%d %H:%M:%OS', ''))");
    
    assertThat(eval("is.na(t$sec)"), elementsIdenticalTo(c(true)));
    assertThat(eval("t$isdst"), elementsIdenticalTo(c_i(-1)));
  }

  @Test
  public void asDate() {

    eval("x <- paste(2004, rep(1:4, 4:1), seq(1,20,2), sep = \"-\")");
    eval("print(x)");
    eval("y <- as.Date(x)");
    eval("print(y)");
    assertThat(eval("y"), elementsIdenticalTo(c(12418d, 12420d, 12422d, 12424d, 12457, 12459, 12461, 12492, 12494, 12527)));
  }
}
