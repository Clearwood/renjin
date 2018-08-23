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
package org.renjin.compiler.ir.tac;

import org.renjin.compiler.ir.tac.functions.TranslationContext;
import org.renjin.sexp.Null;
import org.renjin.sexp.PairList;


public class InlinedContext implements TranslationContext {

  private PairList formals;

  public InlinedContext(PairList formals) {
    this.formals = formals;
  }

  @Override
  public PairList getEllipsesArguments() {
    // We are only supporting the inlining of functions when no arguments are passed via ...
    return Null.INSTANCE;
  }

  public PairList getFormals() {
    return formals;
  }
}
