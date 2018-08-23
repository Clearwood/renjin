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
package org.renjin.primitives.subset;

import org.renjin.eval.Context;
import org.renjin.eval.EvalException;
import org.renjin.sexp.*;

import static org.renjin.primitives.subset.SubsetAssertions.checkBounds;

/**
 * Simple selection using positive or negative indexes of elements in a vector. Any {@code dim} attributes
 * are ignored.
 */
class VectorIndexSelection implements SelectionStrategy {

  private final AtomicVector subscript;

  public VectorIndexSelection(AtomicVector subscript) {
    this.subscript = subscript;
  }

  @Override
  public SEXP getVectorSubset(Context context, Vector source, boolean drop) {
    return buildSelection(source, new IndexSubscript(this.subscript, source.length()), drop);
  }

  public static Vector buildSelection(Vector source, Subscript subscript, boolean drop) {

    IndexIterator it = subscript.computeIndexes();

    Vector.Builder result = source.getVectorType().newBuilder();
    
    AtomicVector sourceNames = source.getNames();
    
    StringArrayVector.Builder resultNames = null;
    if(sourceNames instanceof StringVector) {
      resultNames = new StringArrayVector.Builder();
    }

    int index;
    while((index=it.next())!= IndexIterator.EOF) {
      
      if(IntVector.isNA(index) || index >= source.length()) {
        result.addNA();
        if(resultNames != null) {
          resultNames.addNA();
        }

      } else {
        result.addFrom(source, index);
        if(resultNames != null) {
          resultNames.add(sourceNames.getElementAsString(index));
        }
      }
    }

    // Exceptionally, if the source is a one-dimensional array, then 
    // preserve the dim and dimnames attributes.
    if(isOneDimensionalArray(source) && (!drop || result.length() > 1)) {
      result.setAttribute(Symbols.DIM, new IntArrayVector(result.length()));
      if(resultNames != null) {
        result.setAttribute(Symbols.DIMNAMES, new ListVector(resultNames.build()));
      }
    } else {
      if(resultNames != null) {
        result.setAttribute(Symbols.NAMES, resultNames.build());
      }
    }
    return result.build();
  }

  @Override
  public SEXP getSingleListElement(ListVector source, boolean exact) {
    if(subscript.isElementNA(0)) {
      return Null.INSTANCE;
    }
    IndexSubscript subscript = new IndexSubscript(this.subscript, source.length());

    // verify that we are selecting a single element
    int index = subscript.computeUniqueIndex();
    
    // Note that behavior of NA indices is different for lists than
    // atomic vectors below.
    if(IntVector.isNA(index)) {
      return Null.INSTANCE;
    }
    
    checkBounds(source, index);
    
    return source.getElementAsSEXP(index);
  }

  @Override
  public AtomicVector getSingleAtomicVectorElement(AtomicVector source, boolean exact) {

    IndexSubscript subscript = new IndexSubscript(this.subscript, source.length());

    // verify that we are selecting a single element
    int index = subscript.computeUniqueIndex();
    
    // assert that the index is within bounds
    checkBounds(source, index);
    
    return source.getElementAsSEXP(index);
  }

  @Override
  public SEXP replaceSinglePairListElement(PairList.Node list, SEXP replacement) {
    return replaceSingeListOrPairListElement(
        list.newCopyBuilder(),
        replacement);
  }
  
  @Override
  public ListVector replaceSingleListElement(ListVector source, SEXP replacement) {

    return (ListVector) replaceSingeListOrPairListElement(
        source.newCopyNamedBuilder(), 
        replacement);
  }

  private SEXP replaceSingeListOrPairListElement(ListBuilder result, SEXP replacement) {
    
    // Find the index of the element to replace
    int selectedIndex = new IndexSubscript(subscript, result.length())
        .computeUniqueIndex();
    

    // In the context of the [[<- operator, assign NULL has the effect
    // of deleting an element
    boolean deleting = replacement == Null.INSTANCE;
    boolean exists = (selectedIndex < result.length());
    
    // Otherwise make a copy
    boolean deformed = false;

    if(deleting) {
      if(exists) {
        result.remove(selectedIndex);
        deformed = true;
      }
    } else if(exists) {
      result.set(selectedIndex, replacement);

    } else {
      result.set(selectedIndex, replacement);
      deformed = true;
    }

    // If we've changed the shape of the list, we need to drop matrix-related
    // attributes which are no longer valid
    if(deformed) {
      result.removeAttribute(Symbols.DIM);
      result.removeAttribute(Symbols.DIMNAMES);
    }

    return result.build();
  }


  @Override
  public Vector replaceSingleElement(Context context, AtomicVector source, Vector replacement) {
    
    if(replacement.length() == 0) {
      throw new EvalException("replacement has length zero");
    }

    IndexSubscript subscript = new IndexSubscript(this.subscript, source.length());

    // verify that we are selecting a single element
    subscript.computeUniqueIndex();

    // Build the replacement
    return buildReplacement(context, source, replacement, subscript);
  }

  @Override
  public ListVector replaceListElements(Context context, ListVector source, Vector replacements) {
    IndexSubscript subscript = new IndexSubscript(this.subscript, source.length());
    
    // When replace items on a list, list[i] <- NULL has the meaning of 
    // removing all selected elements
    if(replacements == Null.INSTANCE) {
      return ListSubsetting.removeListElements(source, subscript.computeIndexPredicate());
    }
    
    return (ListVector) buildReplacement(context, source, replacements, subscript);
  }

  @Override
  public Vector replaceAtomicVectorElements(Context context, AtomicVector source, Vector replacements) {
    return buildReplacement(context, source, replacements,  new IndexSubscript(this.subscript, source.length()));
  }

  
  public static Vector buildReplacement(Context context, Vector source, Vector replacements, Subscript subscript) {

    source = context.materialize(source);
    replacements = context.materialize(replacements);

    Vector.Builder builder = source.newCopyBuilder(replacements.getVectorType());
    AtomicVector sourceNames = source.getNames();
    StringVector.Builder resultNames = null;
    if(sourceNames instanceof StringVector) {
      resultNames = ((StringVector) sourceNames).newCopyBuilder();
    }
    
    boolean deformed = false;
    
    int replacementIndex = 0;
    int replacementLength = replacements.length();

    int index;
    IndexIterator it = subscript.computeIndexes();
    while((index=it.next()) != IndexIterator.EOF) {
      
      if(index >= source.length()) {
        deformed = true;
        if(resultNames != null) {
          while(resultNames.length() <= index) {
            resultNames.add("");
          }
        }
      }
      
      if(replacementLength == 0) {
        throw new EvalException("replacement has zero length");
      }
      
      builder.setFrom(index, replacements, replacementIndex++);

      if (replacementIndex >= replacementLength) {
        replacementIndex = 0;
      }
    }
    
    if(deformed) {
      if(resultNames != null) {
        builder.setAttribute(Symbols.NAMES, resultNames.build());
      }
      builder.removeAttribute(Symbols.DIM);
      builder.removeAttribute(Symbols.DIMNAMES);
    }

    return builder.build();
  }

  private static boolean isOneDimensionalArray(Vector source) {
    return source.getAttributes().getDim().length() == 1;
  }

}
