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

import org.renjin.eval.EvalException;
import org.renjin.primitives.Indexes;
import org.renjin.sexp.AtomicVector;
import org.renjin.sexp.IntVector;
import org.renjin.sexp.Vector;

/**
 * Iterates over the sequence of indexes selected by a coordinate matrix.
 */
class CoordinateMatrixIterator implements IndexIterator {

  /**
   * The matrix of coordinates to select. For example:
   * <pre>
   *       [,1] [,2]
   *  [1,]    3    2
   *  [2,]    3    4
   *  [3,]    1    1
   * </pre>   
   * 
   * Each row represents the coordinates of a single element to select.
   */
  private AtomicVector coordinateMatrix;

  /**
   * The number of rows in the coordinate matrix, and the number of
   * elements to select
   */
  private int coordinateRows;

  /**
   * The number of columns in the coordinate matrix. Should be equal to the
   * number of dimensions in the source array. For example, if we are selecting
   * elements from a matrix, the first column will be the row index, and the second will be column index.
   * 
   */
  private int coordinateColumns;

  /**
   * The dimensions of the source matrix/array from which we are selecting elements.
   */
  private int[] sourceDim;

  /**
   * The next row in this iterator's sequence.
   */
  private int nextRow = 0;

  /**
   * A buffer for storing a single row of coordinates.
   */
  private int[] coordRow;

  public CoordinateMatrixIterator(Vector source, AtomicVector coordinateMatrix) {
    this.coordinateMatrix = coordinateMatrix;
    this.sourceDim = source.getAttributes().getDimArray();

    int[] coordinateMatrixDims = coordinateMatrix.getAttributes().getDimArray();
    coordinateRows = coordinateMatrixDims[0];
    coordinateColumns = coordinateMatrixDims[1];
    
    coordRow = new int[sourceDim.length];
    
    // There should be a column in the coordinate matrix for each
    // dimension in the source matrix. 
    // This should be checked much earlier, but double check
    assert coordinateColumns == sourceDim.length : "coordinate matrix shape does not match source array dimensions";
  }

  /**
   * 
   * @return the next index within the source vector indicated by the coordinate matrix.
   */
  @Override
  public int next() {

    rowLoop: while (true) {
      if (nextRow >= coordinateRows) {
        return EOF;
      }

      int coordRow = nextRow++;

      // Extract the coordinate row from the coordinate matrix
      // For example, if we're selecting on a matrix, then
      // we are filling coordRow = [row, column]
      for (int i = 0; i != this.coordRow.length; ++i) {
        int coord = coordinateMatrix.getElementAsInt(
            Indexes.matrixIndexToVectorIndex(coordRow, i, coordinateRows, coordinateColumns));

        // If any of the coordinates are zero, skip this coordinate
        if (coord == 0) {
          continue rowLoop;
        }
        if (IntVector.isNA(coord)) {
          return IntVector.NA;
        }
        if (coord < 0) {
          throw new EvalException("negative values are not allowed in a matrix subscript");
        }
        
        // One-based to zero-based
        this.coordRow[i] = coord - 1;
      }

      // Using the coordinates in coordRow, compute the index within the 
      // source vector
      return Indexes.arrayIndexToVectorIndex(this.coordRow, sourceDim);
    }
  }

  @Override
  public void restart () {
    nextRow = 0;
  }
}
