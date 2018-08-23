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
package org.renjin.primitives.packaging;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.renjin.eval.Context;
import org.renjin.eval.EvalException;
import org.renjin.sexp.NamedValue;
import org.renjin.sexp.Null;
import org.renjin.sexp.SEXP;
import org.renjin.util.NamedByteSource;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class Package {

  private final FqPackageName name;

  protected Package(FqPackageName name) {
    this.name = name;
  }

  public final FqPackageName getName() {
    return name;
  }

  /**
   * Loads the R-language symbols that constitute this package's namespace.
   * 
   * @param context
   * @return
   * @throws IOException
   */
  public Iterable<NamedValue> loadSymbols(Context context) throws IOException {
    return Collections.emptySet();
  }


  public NamedByteSource getResource(String name) throws IOException {
    throw new IOException();
  }

  /**
   * @return the list of datasets contained in this package
   */
  public List<Dataset> getDatasets() {
    return Collections.emptyList();
  }

  public SEXP getDataset(String datasetName) throws IOException {
    for(Dataset dataset : getDatasets()) {
      if(dataset.getName().equals(datasetName)) {
        return dataset.loadAll();
      }
    }
    return Null.INSTANCE;
  }

  /**
   * Loads a JVM class from the package during the construction of the 
   * this package's namespace.
   * 
   * @param name the fully-qualified class name. For example, "java.lang.util.HashSet"
   * @return the {@code Class}
   */
  public abstract Class loadClass(String name) throws ClassNotFoundException;


  /**
   * @return a list of packages that should be loaded into the global search
   * path when this package is loaded. This mechanism predates the more recent
   * NAMESPACE approach.
   */
  public Collection<String> getPackageDependencies() throws IOException {
    return Collections.emptyList();
  }

  /**
   * 
   * @return a VFS {@link FileObject} pointing to the root of the package
   */
  public FileObject resolvePackageRoot(FileSystemManager fileSystemManager) throws FileSystemException {
    throw new EvalException("Cannot access package root");
  }
}
