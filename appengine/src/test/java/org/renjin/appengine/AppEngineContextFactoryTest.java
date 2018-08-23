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
package org.renjin.appengine;
/*
 * R : A Computer Language for Statistical Data Analysis
 * Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (C) 1997--2008  The R Development Core Team
 * Copyright (C) 2003, 2004  The R Foundation
 * Copyright (C) 2010 bedatadriven
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */



import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;
import org.junit.Test;
import org.renjin.eval.Context;
import org.renjin.eval.Session;
import org.renjin.eval.SessionBuilder;
import org.renjin.sexp.Symbol;
import org.renjin.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class AppEngineContextFactoryTest {

  @Test
  public void rootFile() throws IOException {
    DefaultLocalFileProvider localFileProvider = new DefaultLocalFileProvider();
    FileSystemManager fsm = AppEngineContextFactory.createFileSystemManager(localFileProvider);

    Session session = new SessionBuilder()
        .withFileSystemManager(fsm)
        .build();
    
    session.setWorkingDirectory(FileSystemUtils.workingDirectory(fsm));
   
    Context context = session.getTopLevelContext();

    context.evaluate( Symbol.get("search") );
  }

  @Test
  public void homeDirectory() throws IOException {
    String resourcePath = "file:/base/app/1.234234/WEB-INF/lib/renjin-core-0.1.0-SNAPSHOT.jar!/org/renjin/sexp/SEXP.class";
    String home = AppEngineContextFactory.findHomeDirectory(
        new File("/base/app/1.234234"), resourcePath);

    assertThat( home, equalTo("jar:file:///WEB-INF/lib/renjin-core-0.1.0-SNAPSHOT.jar!/org/renjin"));
  }

}
