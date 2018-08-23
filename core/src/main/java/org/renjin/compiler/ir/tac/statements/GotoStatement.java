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
package org.renjin.compiler.ir.tac.statements;

import org.renjin.compiler.codegen.EmitContext;
import org.renjin.compiler.ir.tac.IRLabel;
import org.renjin.compiler.ir.tac.expressions.Expression;
import org.renjin.compiler.ir.tac.expressions.NullExpression;
import org.renjin.repackaged.asm.Opcodes;
import org.renjin.repackaged.asm.commons.InstructionAdapter;

import java.util.Arrays;


public class GotoStatement implements Statement, BasicBlockEndingStatement {

  private final IRLabel target;

  public GotoStatement(IRLabel target) {
    this.target = target;
  }

  public IRLabel getTarget() {
    return target;
  }

  
  @Override
  public Iterable<IRLabel> possibleTargets() {
    return Arrays.asList(target);
  }

  @Override
  public String toString() {
    return "goto " + target;
  }


  @Override
  public Expression getRHS() {
    return NullExpression.INSTANCE;
  }

  @Override
  public void setRHS(Expression newRHS) {
    if(newRHS != NullExpression.INSTANCE) {
      throw new IllegalArgumentException();
    }
  }


  @Override
  public void setChild(int childIndex, Expression child) {
    throw new IllegalArgumentException();
  }

  @Override
  public int getChildCount() {
    return 0;
  }

  @Override
  public Expression childAt(int index) {
    throw new IllegalArgumentException();
  }

  @Override
  public void accept(StatementVisitor visitor) {
    visitor.visitGoto(this);
  }

  @Override
  public int emit(EmitContext emitContext, InstructionAdapter mv) {
    mv.visitJumpInsn(Opcodes.GOTO, emitContext.getAsmLabel(target));
    return 0;
  }

  @Override
  public boolean isPure() {
    return true;
  }
}
