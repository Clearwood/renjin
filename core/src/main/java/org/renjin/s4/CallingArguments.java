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
package org.renjin.s4;

import org.renjin.eval.ArgumentMatcher;
import org.renjin.eval.Calls;
import org.renjin.eval.Context;
import org.renjin.eval.MatchedArguments;
import org.renjin.sexp.*;

import java.util.Iterator;

/**
 * Prepares the arguments to the call for matching.
 *
 */
public class CallingArguments {

  private final Context context;
  private final PairList promisedArgs;

  private CallingArguments(Context context, PairList promisedArgs) {
    this.context = context;
    this.promisedArgs = promisedArgs;
  }


  public static CallingArguments primitiveArguments(Context context, Environment rho, ArgumentMatcher matcher, SEXP object, PairList args) {

    // expand ... in arguments or remove if empty
    PairList expandedArgs = Calls.promiseArgs(args, context, rho);


    // when length of all arguments used in function call is as long as formals length (except ...)
    // then arguments are matched positionally and the argument tags are ignored
    // Example:
    // > setClass("ABC", representation(x="numeric"))
    // > obj <- new("ABC")

    // Matched by position:
    //   > obj[[i=1,j="hello"]] <- 100
    //   `[[` formals are x(i, j, ..., value)
    //   obj signature is:   x=ABC, i=numeric, j=character, value=numeric
    //
    // Matched by position:
    //   > obj[[j=1,i="hello"] <- 100
    //   obj signature is:   x=ABC, i=numeric, j=character, value=numeric
    //
    // Matched by *name* because there is no match for the formal 'i' :
    //   > obj[[j=1]] <- 100
    //   obj signature is:   x=ABC, i=missing, j=numeric,   value=numeric


    PairList promisedArgs;
    if(matcher.getNamedFormalCount() == expandedArgs.length()) {

      // Match positionally, ignoring the names of the arguments.
      promisedArgs = expandedArgs;


    } else {

      // Match the provided arguments to the formals of the generic

      MatchedArguments matchedArguments = matcher.match(expandedArgs);
      promisedArgs = matchByName(rho, object, matchedArguments);

    }
    return new CallingArguments(context, promisedArgs);
  }


  public static CallingArguments standardGenericArguments(Context context, Environment callingEnvironment, ArgumentMatcher argumentMatcher) {
    PairList.Builder promisedArgs = new PairList.Builder();
    for (String formalName : argumentMatcher.getFormalNames()) {
      SEXP promisedArg = callingEnvironment.getVariableUnsafe(formalName);
      promisedArgs.add(formalName, promisedArg);
    }
    return new CallingArguments(context, promisedArgs.build());
  }

  private static PairList matchByName(Environment rho, SEXP object, MatchedArguments matchedArguments) {
    PairList.Builder promisedArgs = new PairList.Builder();
    for (int formalIndex = 0; formalIndex < matchedArguments.getFormalCount(); formalIndex++) {

      Symbol formalName = matchedArguments.getFormalName(formalIndex);
      int actualIndex = matchedArguments.getActualIndex(formalIndex);

      if(actualIndex == -1) {
        if(formalName != Symbols.ELLIPSES) {
          // This formal argument was not provided by the caller
          promisedArgs.add(formalName, Symbol.MISSING_ARG);
        }
      } else {
        SEXP uneval = matchedArguments.getActualValue(actualIndex);
        if(actualIndex == 0) {
          // The source has already been evaluated to check for class
          promisedArgs.add(formalName, new Promise(uneval, object));

        } else {
          promisedArgs.add(formalName, Promise.repromise(rho, uneval));
        }
      }
    }
    return promisedArgs.build();
  }

  public PairList getPromisedArgs() {
    return promisedArgs;
  }

  public Signature getSignature(int length) {
    String[] classes = new String[length];
    Iterator<PairList.Node> argumentIt = promisedArgs.nodes().iterator();
    for(int index = 0; index < length; ++index) {
      if(argumentIt.hasNext()) {
        SEXP actual = argumentIt.next().getValue();
        SEXP evaluated = actual.force(context);
        if (evaluated == Symbol.MISSING_ARG) {
          classes[index] = "missing";
        } else {
          classes[index] = computeDateClass(evaluated);
        }
      } else {
        classes[index] = "missing";
      }
    }
    return new Signature(classes);
  }

  private String computeDateClass(SEXP evaluated) {
    AtomicVector classAttribute = evaluated.getAttributes().getClassVector();
    if (classAttribute.length() > 0) {
      /*
       * S3 Class has been explicitly defined
       */
      return classAttribute.getElementAsString(0);

    } else {
      /*
       * Compute implicit class based on DIM attribute and type
       */
      Vector dim = evaluated.getAttributes().getDim();
      if (dim.length() == 2) {
        return "matrix";
      } else if (dim.length() > 0) {
        return "array";
      } else if (evaluated instanceof IntVector) {
        return "integer";
      } else if (evaluated instanceof DoubleVector) {
        return "numeric";
      } else {
        return evaluated.getImplicitClass();
      }
    }
  }
}
