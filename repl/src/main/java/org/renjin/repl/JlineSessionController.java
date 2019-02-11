/*
 * Renjin : JVM-based interpreter for the R language for the statistical analysis
 * Copyright © 2010-2019 BeDataDriven Groep B.V. and contributors
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
package org.renjin.repl;

import jline.Terminal;
import jline.console.ConsoleReader;
import org.renjin.eval.Context;
import org.renjin.eval.EvalException;
import org.renjin.eval.SessionController;
import org.renjin.sexp.StringVector;

import java.io.IOException;

public class JlineSessionController extends SessionController {

  private final Terminal terminal;
  private final ConsoleReader reader;
  private boolean interactive = true;


  public JlineSessionController(ConsoleReader reader) {
    super();
    this.terminal = reader.getTerminal();
    this.reader = reader;
  }

  @Override
  public void quit(Context context, SaveMode saveMode, int exitCode,
      boolean runLast) {
    throw new QuitException(exitCode);
  }

  @Override
  public boolean isInteractive() {
    return interactive;
  }

  public void setInteractive(boolean interactive) {
    this.interactive = interactive;
  }

  @Override
  public int menu(StringVector choices) {
    for(int i=0;i!=choices.length();++i) {
      System.out.println(i + ": " + choices.getElementAsString(i));
    }
    return 0;
  }

  @Override
  public String readLine(String prompt) {

    if(!interactive) {
      return "";
    }

    try {
      return reader.readLine(prompt);
    } catch (IOException e) {
      throw new EvalException("IOException reading input: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean isTerminal() {
    return terminal.isAnsiSupported();
  }
}
