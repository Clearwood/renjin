package org.renjin.primitives.packaging;

import org.renjin.eval.EvalException;
import org.renjin.sexp.Environment;
import org.renjin.sexp.SEXP;
import org.renjin.sexp.Symbol;

public class Namespace {
  
  private String name;
  private final NamespaceDef def;
  private final Environment namespaceEnvironment;
    
  public Namespace(NamespaceDef namespaceDef, String name, Environment namespaceEnvironment) {
    this.name = name;
    this.def = namespaceDef;
    this.namespaceEnvironment = namespaceEnvironment;
  }
  
  public String getName() {
    return name;
  }

  public SEXP getEntry(Symbol entry) {
    throw new EvalException("unimplemented");
  }

  public SEXP getExport(Symbol entry) {
    throw new EvalException("unimplemented");
  }

  public Environment getNamespaceEnvironment() {
    return this.namespaceEnvironment;
  }

  public void copyExportsTo(Environment packageEnv) {
    for(Symbol name : def.getExports()) {
      packageEnv.setVariable(name, namespaceEnvironment.getVariable(name));
    }
  }
}
