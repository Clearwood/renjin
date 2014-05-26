package org.renjin.primitives.packaging;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import org.renjin.eval.Context;
import org.renjin.packaging.LazyLoadFrame;
import org.renjin.primitives.io.serialization.RDataReader;
import org.renjin.sexp.NamedValue;
import org.renjin.sexp.SEXP;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Implements a standard package layout used by renjin's tools
 *
 */
public abstract class FileBasedPackage extends Package {


  protected FileBasedPackage(FqPackageName name) {
    super(name);
  }

  @Override
  public Iterable<NamedValue> loadSymbols(Context context) throws IOException {
    return LazyLoadFrame.load(context, new Function<String, InputStream>() {

      @Override
      public InputStream apply(String name) {
        try {
          return getResource(name).openStream();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  public abstract boolean resourceExists(String name);


  private Properties readDatasetIndex() throws IOException {
    Properties datasets = new Properties();
    if(resourceExists("datasets")) {
      InputStream in = getResource("datasets").openStream();
      try {
        datasets.load(in);
      } finally {
        Closeables.closeQuietly(in);
      }
    }
    return datasets;
  }

  @Override
  public Collection<String> getPackageDependencies() throws IOException {
    if(resourceExists("requires")) {
      ImmutableList<String> lines = getResource("requires").asCharSource(Charsets.UTF_8).readLines();
      List<String> dependencies = Lists.newArrayList();
      for(String line : lines) {
        if(!Strings.isNullOrEmpty(line)) {
          dependencies.add(line);
        }
      }
      return dependencies;

    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public List<Dataset> getDatasets() {
    try {
      Properties index = readDatasetIndex();
      List<Dataset> datasets = Lists.newArrayList();
      for(String logicalDatasetName : index.stringPropertyNames()) {
        datasets.add(new FileBasedDataset(logicalDatasetName, 
            index.getProperty(logicalDatasetName).split("\\s*,\\s*")));
      }
      return datasets;
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }


  private class FileBasedDataset extends Dataset {

    private String datasetName;
    private List<String> objectNames;

    public FileBasedDataset(String name, String[] objectNames) {
      this.datasetName = name;
      this.objectNames = Arrays.asList(objectNames);
    }

    @Override
    public String getName() {
      return datasetName;
    }

    @Override
    public Collection<String> getObjectNames() {
      return objectNames;
    }

    @Override
    public SEXP loadObject(String name) throws IOException {
      if(!objectNames.contains(name)) {
        throw new IllegalArgumentException(name);
      }
      InputStream in = getResource("data/" + name).openStream();
      try {
        RDataReader reader = new RDataReader(in);
        return reader.readFile();
      } finally {
        Closeables.closeQuietly(in);
      }
    }
  }
}
