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
package org.renjin.compiler.codegen;


import org.renjin.compiler.TypeSolver;
import org.renjin.compiler.cfg.BasicBlock;
import org.renjin.compiler.cfg.ControlFlowGraph;
import org.renjin.compiler.ir.tac.IRLabel;
import org.renjin.compiler.ir.tac.expressions.Expression;
import org.renjin.compiler.ir.tac.expressions.LValue;
import org.renjin.compiler.ir.tac.statements.Assignment;
import org.renjin.compiler.ir.tac.statements.Statement;
import org.renjin.repackaged.asm.Label;
import org.renjin.repackaged.asm.Type;
import org.renjin.repackaged.asm.commons.InstructionAdapter;
import org.renjin.repackaged.guava.collect.HashMultimap;
import org.renjin.repackaged.guava.collect.Maps;
import org.renjin.repackaged.guava.collect.Multimap;
import org.renjin.sexp.*;

import java.util.Map;

public class EmitContext {

  private Map<IRLabel, Label> labels = Maps.newHashMap();
  private Multimap<LValue, Expression> definitionMap = HashMultimap.create();
  private int paramSize;
  private VariableSlots variableSlots;
  
  private int loopVectorIndex;
  private int loopIterationIndex;

  private int maxInlineVariables;

  public EmitContext(ControlFlowGraph cfg, int paramSize, VariableSlots variableSlots) {
    this.paramSize = paramSize;
    this.variableSlots = variableSlots;
    buildDefinitionMap(cfg);
  }

  public int getContextVarIndex() {
    return 1;
  }
  public int getEnvironmentVarIndex() {
    return 2;
  }

  private void buildDefinitionMap(ControlFlowGraph cfg) {
    for(BasicBlock bb : cfg.getBasicBlocks()) {
      for(Statement stmt : bb.getStatements()) {
        if(stmt instanceof Assignment) {
          Assignment assignment = (Assignment)stmt;
          definitionMap.put(assignment.getLHS(), assignment.getRHS());
        }
      }
    }
  }

  public Label getAsmLabel(IRLabel irLabel) {
    Label asmLabel = labels.get(irLabel);
    if(asmLabel == null) {
      asmLabel = new Label();
      labels.put(irLabel, asmLabel);
    }
    return asmLabel;
  }

  /**
   * Creates a new {@code EmitContext} that allocates local variables in the same context, and overrides
   * {@link #writeReturn(InstructionAdapter, Type)} to jump to the end of the inlined function rather than
   * actually returning.
   */
  public InlineEmitContext inlineContext(ControlFlowGraph cfg, TypeSolver types) {
    VariableSlots childSlots = new VariableSlots(paramSize + variableSlots.getNumLocals(), types);
    if(childSlots.getNumLocals() > maxInlineVariables) {
      maxInlineVariables = childSlots.getNumLocals();
    }
    return new InlineEmitContext(cfg,
        EmitContext.this.paramSize +
        EmitContext.this.variableSlots.getNumLocals(),
        childSlots);
  }

  public int getLoopVectorIndex() {
    return loopVectorIndex;
  }

  public void setLoopVectorIndex(int loopVectorIndex) {
    this.loopVectorIndex = loopVectorIndex;
  }

  public int getLoopIterationIndex() {
    return loopIterationIndex;
  }

  public void setLoopIterationIndex(int loopIterationIndex) {
    this.loopIterationIndex = loopIterationIndex;
  }

  public int getRegister(LValue lValue) {
    return variableSlots.getSlot(lValue);
  }

  public int convert(InstructionAdapter mv, Type fromType, Type toType) {

    if(fromType.equals(Type.getType(DoubleArrayVector.class))) {
      fromType = Type.getType(DoubleVector.class);
    }

    if(fromType.equals(toType)) {
      // NOOP
      return 0;


    } else if(fromType.getSort() != Type.OBJECT && toType.getSort() != Type.OBJECT) {
      // Simple primitive conversion
      mv.cast(fromType, toType);
      return 0;
      
    } else if(fromType.equals(Type.getType(SEXP.class)) || fromType.equals(Type.getType(DoubleVector.class))) {
      // FROM SEXP -> .....
      if (toType.getSort() == Type.OBJECT) {
        mv.checkcast(toType);
        return 0;

      } else if (toType.equals(Type.DOUBLE_TYPE)) {
        mv.invokeinterface(Type.getInternalName(SEXP.class), "asReal",
            Type.getMethodDescriptor(Type.DOUBLE_TYPE));
        return 0;

      } else if (toType.equals(Type.INT_TYPE)) {
        mv.checkcast(Type.getType(Vector.class));
        mv.iconst(0);
        mv.invokeinterface(Type.getInternalName(Vector.class), "getElementAsInt",
            Type.getMethodDescriptor(Type.INT_TYPE, Type.INT_TYPE));
        return 1;

      }

    } else if(toType.equals(Type.getType(AtomicVector.class))) {
      // TO DOUBLE VECTOR

      if (fromType.equals(Type.getType(DoubleVector.class))) {
        // noop
        return 0;
      }

    } else if(toType.equals(Type.getType(SEXP.class))) {
      // TO SEXP --->
      
      if(fromType.getSort() == Type.OBJECT) {
        // No cast necessary
        return 0;
      }
      
      switch (fromType.getSort()) {
        case Type.INT:
          return box(mv, IntVector.class, Type.INT_TYPE);
        
        case Type.DOUBLE:
          return box(mv, DoubleVector.class, Type.DOUBLE_TYPE);
        
      }
    }
    
    throw new UnsupportedOperationException("Unsupported conversion: " + fromType + " -> " + toType);
  }
  
  private int box(InstructionAdapter mv, Class vectorClass, Type primitiveType) {
    mv.invokestatic(Type.getInternalName(vectorClass), "valueOf",
        Type.getMethodDescriptor(Type.getType(vectorClass), primitiveType), false);
    return 0;
  }

  public VariableStorage getVariableStorage(LValue lhs) {
    return variableSlots.getStorage(lhs);
  }

  public int getLocalVariableCount() {
    return paramSize + variableSlots.getNumLocals();
  }

  public void writeReturn(InstructionAdapter mv, Type returnType) {
    mv.areturn(returnType);
  }

  public void writeDone(InstructionAdapter mv) {

  }

  public void loadParam(InstructionAdapter mv, Symbol param) {
    throw new IllegalStateException();
  }

}
