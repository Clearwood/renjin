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
package org.renjin.primitives.matrix;

import org.renjin.eval.Context;
import org.renjin.eval.EvalException;
import org.renjin.invoke.annotations.Builtin;
import org.renjin.invoke.annotations.Current;
import org.renjin.invoke.annotations.Generic;
import org.renjin.invoke.annotations.Internal;
import org.renjin.primitives.Indexes;
import org.renjin.primitives.Warning;
import org.renjin.primitives.sequence.RepDoubleVector;
import org.renjin.primitives.sequence.RepLogicalVector;
import org.renjin.primitives.vector.ComputingIntVector;
import org.renjin.sexp.*;


/**
 * Implementation of R primitive functions involving matrices.
 */
public class Matrices {

  private Matrices() {}

  @Internal("t.default")
  public static Vector transpose(Vector x) {
    Vector dimensions = x.getAttributes().getDim();
 
    if(dimensions.length() == 0) {
      return transposeVector(x);

    } else if(dimensions.length() == 1) {
      return transposeArray(x);
      
    } else if(dimensions.length() == 2) {
      int nrows = dimensions.getElementAsInt(0);
      int ncols = dimensions.getElementAsInt(1);

      /*
       * Transpose the attributes
       */
      AttributeMap.Builder attributes = new AttributeMap.Builder();
      attributes.setDim(ncols, nrows);
      attributes.set(Symbols.DIMNAMES, transposeDimNames(x));

      /*
       * Transpose the values
       */
      if(x instanceof DoubleVector && x.length() > TransposingMatrix.LENGTH_THRESHOLD) {
        // Just wrap the matrix
        return new TransposingMatrix(x, attributes.build());

      } else {
        // actually allocate the memory
        Vector.Builder builder = x.newBuilderWithInitialSize(x.length());

        for (int i = 0; i < nrows; i++) {
          for (int j = 0; j < ncols; j++) {
            builder.setFrom(Indexes.matrixIndexToVectorIndex(j, i, ncols, nrows), x,
                    Indexes.matrixIndexToVectorIndex(i, j, nrows, ncols));
          }
        }
        return (Vector)builder.build().setAttributes(attributes.build());
      }
    } else {
      throw new EvalException("argument is not a matrix");
    }
  }

  /**
   * Treat a vector {@code x} as a matrix of {@code length(x)} rows, and transpose
   * it to a matrix of {@code length(x)} columns.
   */
  private static Vector transposeVector(Vector x) {
    AttributeMap.Builder matrixDims = AttributeMap.builder();
    matrixDims.setDim(1, x.length());
    
    // Treat any names present as 
    if(x.getNames() != Null.INSTANCE) {
      matrixDims.setDimNames(new ListVector(Null.INSTANCE, x.getNames()));
    }

    // Copy any remaining attributes
    for (Symbol attribute : x.getAttributes().names()) {
      if (attribute != Symbols.NAMES &&
          attribute != Symbols.DIM &&
          attribute != Symbols.DIMNAMES) {
        matrixDims.set(attribute, x.getAttribute(attribute));
      }
    }
    return (Vector)x.setAttributes(matrixDims);
  }

  /**
   * Treat a 1-dimensional array {@code x} as a matrix of {@code length(x)} rows, and transpose
   * it to a matrix of {@code length(x)} columns.
   */
  private static Vector transposeArray(Vector x) {
    AttributeMap.Builder matrixDims = AttributeMap.builder();
    matrixDims.setDim(1, x.length());

    Vector dimNameList = x.getAttributes().getDimNames();
    if(dimNameList != Null.INSTANCE) {
      AttributeMap.Builder dimNameAttributes = AttributeMap.builder();
      Vector rowNames = Null.INSTANCE;
      Vector colNames = dimNameList.getElementAsSEXP(0);
      
      if(dimNameList.getNames() != Null.INSTANCE) {
        String name = dimNameList.getNames().getElementAsString(0);
        dimNameAttributes.setNames(new StringArrayVector("", name));
      }
      matrixDims.setDimNames(new ListVector(new SEXP[] { rowNames, colNames}, dimNameAttributes.build()));
    }
    
    // Copy any remaining attributes
    for (Symbol attribute : x.getAttributes().names()) {
      if(attribute != Symbols.NAMES &&
          attribute != Symbols.DIM &&
          attribute != Symbols.DIMNAMES) {
        matrixDims.set(attribute, x.getAttribute(attribute));
      }
    }
    return (Vector)x.setAttributes(matrixDims);
  }


  /**
   * Transpose dimension names if present.
   */
  private static SEXP transposeDimNames(Vector x) {
    Vector dimNames = x.getAttributes().getDimNames();
    if(dimNames == Null.INSTANCE) {
      return Null.INSTANCE;
      
    } else {
      ListVector dimNameList = (ListVector) dimNames;
      SEXP rowNames = dimNameList.getElementAsSEXP(0);
      SEXP colNames = dimNameList.getElementAsSEXP(1);

      // Transpose the names of the dimnames
      AttributeMap.Builder transposedAttributes = AttributeMap.builder();
      if(dimNameList.getNames() != Null.INSTANCE) {
        transposedAttributes.setNames(new StringArrayVector(dimNameList.getName(1), dimNameList.getName(0)));
      }
      return new ListVector(new SEXP[] { colNames, rowNames }, transposedAttributes.build());
    }
  }

  @Builtin("%*%")
  @Generic(S3 = false, S4 = true)
  public static SEXP matrixproduct(AtomicVector x, AtomicVector y) {
    return new MatrixProduct(MatrixProduct.PROD, x, y).compute();
  }

  @Internal("crossprod")
  public static SEXP crossprod(AtomicVector x, AtomicVector y) {
    return new MatrixProduct(MatrixProduct.CROSSPROD, x, y).compute();
  }

  @Internal("tcrossprod")
  public static SEXP tcrossprod(AtomicVector x, AtomicVector y) {
    return new MatrixProduct(MatrixProduct.TCROSSPROD, x, y).compute();
  }

  @Internal
  public static DoubleVector rowSums(AtomicVector x, int numRows, int rowLength, boolean naRm) {
    double sums[] = new double[numRows];
    int sourceIndex = 0;
    for(int col=0;col < rowLength; col++) {
      for(int row=0;row<numRows;row++) {
        double value = x.getElementAsDouble(sourceIndex++);
        if(!naRm) {
          sums[row] += value;
        } else if(!Double.isNaN(value)) {
          sums[row] += value;
        }
      }
    }

    return DoubleArrayVector.unsafe(sums);
  }

  @Internal
  public static DoubleVector rowMeans(AtomicVector x,
                                      int numRows,
                                      int rowLength,
                                      boolean naRm) {
    if(!naRm && x.isDeferred()) {
      return new DeferredRowMeans(x, numRows, AttributeMap.EMPTY);
    }

    double sums[] = new double[numRows];
    int counts[] = new int[numRows];
    int sourceIndex = 0;
    for(int col=0;col < rowLength; col++) {
      for(int row=0;row<numRows;row++) {
        double value = x.getElementAsDouble(sourceIndex++);
        if(!naRm) {
          sums[row] += value;
          counts[row] ++;
        } else if(!Double.isNaN(value)) {
          sums[row] += value;
          counts[row] ++;
        }
      }
    }
    for(int row=0;row<numRows;row++) {
      sums[row] /= (double)counts[row];
    }
    return DoubleArrayVector.unsafe(sums);
  }

  @Internal
  public static DoubleVector colSums(AtomicVector x, int columnLength, int numColumns, boolean naRm) {
    DeferredColSums dcs =  new DeferredColSums(x, numColumns, naRm, AttributeMap.EMPTY);
    if (System.getProperty("renjin.disable.colsums") != null) {
      return (DoubleVector) dcs.forceResult();
    }
    return dcs;
  }

  @Internal
  public static DoubleVector colMeans(AtomicVector x, int columnLength, int numColumns, boolean naRm) {
    double sums[] = new double[numColumns];
    int counts[] = new int[numColumns];

    for(int column=0;column < numColumns; column++) {
      int sourceIndex = columnLength*column;

      double sum = 0;
      int count = 0;
      for(int row=0;row < columnLength; ++row) {
        double cellValue = x.getElementAsDouble(sourceIndex++);
        if(Double.isNaN(cellValue)) {
          if(!naRm) {
            sum = DoubleVector.NA;
            break;
          }
        } else {
          sum += cellValue;
          count++;
        }
      }
      sums[column] = sum;
      counts[column] = count;
    }

    for(int i=0;i!=sums.length;++i) {
      sums[i] = sums[i] / (double)counts[i];
    }

    return new DoubleArrayVector(sums);
  }

  /**
   * Transpose an array by permuting its dimensions and optionally resizing it.
   * @param source the array to be transposed.
   * @param permutationVector the subscript permutation vector, which must be a permutation of the
   *      integers 1:n, where {@code n} is the number of dimensions of {@code source}.
   * @param resize flag indicating whether the vector should be resized as well as having its elements reordered
   * @return A transposed version of array a, with subscripts permuted as indicated by the array perm.
   * If resize is TRUE, the array is reshaped as well as having
   *  its elements permuted, the dimnames are also permuted; if resize = FALSE then the returned
   * object has the same dimensions as a, and the dimnames are dropped. In each case other attributes
   * are copied from a.
   */
  @Internal
  public static SEXP aperm(Vector source, AtomicVector permutationVector, boolean resize) {
    if(!resize) {
      throw new UnsupportedOperationException("resize=TRUE not yet implemented");
    }

    SEXP dimExp = source.getAttributes().getDim();
    EvalException.check(dimExp instanceof IntVector, "invalid first argument, must be an array");
    int permutation[] = toPermutationArray(permutationVector);

    if(isIdentityPermutation(permutation)) {
      /*
       * No actual change to the vector
       */
      return source;

    } else if(source instanceof DoubleVector && isMatrixTransposition(permutation) &&
            source.length() > TransposingMatrix.LENGTH_THRESHOLD) {

      /*
       * This is equivalent to a transposition, use
       * the faster transpose method
       */
      return transpose(source);

    } else {
      /*
       * Actually perform the permutation here
       */

      int dim[] = ((IntVector) dimExp).toIntArray();
      int permutedDims[] = Indexes.permute(dim, permutation);

      Vector.Builder newVector = source.newBuilderWithInitialSize(source.length());
      int index[] = new int[dim.length];
      for(int i=0;i!=newVector.length();++i) {
        Indexes.vectorIndexToArrayIndex(i, index, dim);
        index = Indexes.permute(index, permutation);
        int newIndex = Indexes.arrayIndexToVectorIndex(index, permutedDims);
        newVector.setFrom(newIndex, source, i);
      }

      newVector.setAttribute(Symbols.DIM, new IntArrayVector(permutedDims));

      for(PairList.Node node : source.getAttributes().nodes()) {
        if(node.getTag().equals(Symbols.DIM)) {

        } else if(node.getTag().equals(Symbols.DIMNAMES)) {
          newVector.setAttribute(node.getName(), Indexes.permute((Vector) node.getValue(), permutation));
        } else {
          newVector.setAttribute(node.getName(), node.getValue());
        }
      }
      return newVector.build();
    }
  }

  public static boolean isMatrixTransposition(int[] permutation) {
    return permutation.length == 2 && permutation[0] == 1 && permutation[1] == 0;
  }

  public static boolean isIdentityPermutation(int[] permutation) {
    for(int i=0;i!=permutation.length;++i) {
      if(permutation[i] != i) {
        return false;
      }
    }
    return true;
  }

  /**
   *
   * @param vector User-supplied 1-based permutation vector
   * @return  zero-based permutation int[]
   */
  public static int[] toPermutationArray(Vector vector) {
    int values[] = new int[vector.length()];
    for(int i=0;i!=values.length;++i) {
      values[i] = vector.getElementAsInt(i) - 1;
    }
    return values;
  }

  /**
   * Creates a matrix from the given set of values.
   *
   * @param data an optional data vector.
   * @param nrow the desired number of rows.
   * @param ncol the desired number of columns.
   * @param byRow If FALSE (the default) the matrix is filled by columns, otherwise the matrix is filled by rows.
   * @return
   */
  @Internal
  public static Vector matrix(@Current Context context,
                              Vector data,
                              int nrow, int ncol,
                              boolean byRow,
                              Vector dimnames,
                              boolean nrowMissing, boolean ncolMissing) {

    if(data == Null.INSTANCE) {
      throw new EvalException("'data' must be of vector type, was 'NULL'");
    }
    
    int dataLength = data.length();

    // When matrix() is called with nrow or ncol arguments that are themselves
    // function arguments with default values 
    if(nrow != 1) {
      nrowMissing = false;
    }
    if(ncol != 1) {
      ncolMissing = false;
    }
    
    
    if (nrowMissing && ncolMissing) {
      nrow = dataLength;
    } else if(nrowMissing) {
      nrow = (int)Math.ceil(dataLength / (double)ncol);
    } else if(ncolMissing) {
      ncol = (int)Math.ceil(dataLength / (double)nrow);
    }

    if(dataLength > 0) {
      if (dataLength > 1 && (nrow * ncol) % dataLength != 0) {
        if (((dataLength > nrow) && (dataLength / nrow) * nrow != dataLength) ||
                ((dataLength < nrow) && (nrow / dataLength) * dataLength != nrow)) {

          Warning.invokeWarning(context,
                  "data length [%d] is not a sub-multiple or multiple of the number of rows [%d]",
                  dataLength, nrow);

        } else if (((dataLength > ncol) && (dataLength / ncol) * ncol != dataLength) ||
                ((dataLength < ncol) && (ncol / dataLength) * dataLength != ncol)) {

          Warning.invokeWarning(context,
                  "data length [%d] is not a sub-multiple or multiple of the number of columns [%d]",
                  dataLength, ncol);
        }
      }  else if ((dataLength > 1) && (nrow * ncol == 0)){
        Warning.invokeWarning(context,
                "data length exceeds size of matrix");
      }
    }

    AttributeMap attributes = AttributeMap
            .builder()
            .setDim(nrow, ncol)
            .set(Symbols.DIMNAMES, dimnames)
            .build();

    /*
     * Avoid allocating huge arrays of data
     */
    int resultLength = (nrow * ncol);
    if(!byRow && data instanceof AtomicVector && resultLength == data.length()) {
      return (Vector)data.setAttributes(attributes);
    }
    if(!byRow && resultLength > 500) {
      if(data instanceof DoubleVector) {
        return new RepDoubleVector(data, resultLength, 1, attributes);
      }
    }
    return allocMatrix(data, nrow, ncol, byRow, dimnames);
  }

  private static Vector allocMatrix(Vector data, int nrow, int ncol, boolean byRow, Vector dimnames) {
    Vector.Builder result = null;
    int dataLength = data.length();
    int outLength = nrow * ncol;

    if (dataLength == 1 && outLength > 0 && data instanceof LogicalVector) {
      /* If data has only one entry, we can get away with a constant. 
       * This is true for the common case of matrix(nrow=42, ncol=42)  */
      result = RepLogicalVector.newConstantBuilder(data.getElementAsLogical(0), outLength);
    } else {
      result = data.newBuilderWithInitialSize(nrow * ncol);
      if(dataLength > 0) {
        int i = 0;
        if (!byRow) {
          for (int col = 0; col < ncol; ++col) {
            for (int row = 0; row < nrow; ++row) {
              int sourceIndex = Indexes.matrixIndexToVectorIndex(row, col, nrow, ncol) % dataLength;
              result.setFrom(i++, data, sourceIndex);
            }
          }
        } else {
          for (int row = 0; row < nrow; ++row) {
            for (int col = 0; col < ncol; ++col) {
              result.setFrom(row + (col * nrow), data, i % dataLength);
              i++;
            }
          }
        }
      }
    }
    
    result.setDim(nrow, ncol);
    result.setAttribute(Symbols.DIMNAMES, dimnames);
    return result.build();
  }

  @Internal
  public static IntVector row(IntVector dims){
    if(dims.length()!=2){
      throw new EvalException("a matrix-like object is required as argument to 'row/col'");
    }
    final int rows = dims.getElementAsInt(0);
    final int cols = dims.getElementAsInt(1);

    ComputingIntVector.Functor fn = new ComputingIntVector.Functor() {
      @Override
      public int apply(int index) {
        return (index % rows) + 1;
      }
    };

    return new ComputingIntVector(fn, rows * cols, AttributeMap.dim(rows, cols));
  }

  @Internal
  public static IntVector col(IntVector dims) {
    if(dims.length()!=2){
      throw new EvalException("a matrix-like object is required as argument to 'row/col'");
    }
    final int rows = dims.getElementAsInt(0);
    final int cols = dims.getElementAsInt(1);

    ComputingIntVector.Functor fn = new ComputingIntVector.Functor() {
      @Override
      public int apply(int index) {
        return (index / rows) + 1;
      }
    };
    return new ComputingIntVector(fn, rows * cols, AttributeMap.dim(rows, cols));
  }
}
