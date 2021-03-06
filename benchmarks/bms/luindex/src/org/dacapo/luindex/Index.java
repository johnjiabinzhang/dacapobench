/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Apache Software Foundation - org.apache.lucene.demo.IndexFiles
 *     Australian National University - adaptation to DaCapo test harness
 */
package org.dacapo.luindex;

/**

 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.nio.file.Paths;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;

/**
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: Index.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Index {

  private final File scratch;

  public Index(File scratch) {
    this.scratch = scratch;
  }

  /**
   * Index all text files under a directory.
   */
  public void main(final File INDEX_DIR, final String[] args) throws IOException {
    IndexWriterConfig IWConfig = new IndexWriterConfig();
    IWConfig.setOpenMode (IndexWriterConfig.OpenMode.CREATE);
    IWConfig.setMergePolicy (new LogByteSizeMergePolicy());
    IndexWriter writer = new IndexWriter(FSDirectory.open(Paths.get(INDEX_DIR.getCanonicalPath())), IWConfig);
    for (int arg = 0; arg < args.length; arg++) {
      final File docDir = new File(args[arg]);
      if (!docDir.exists() || !docDir.canRead()) {
        System.out.println("Document directory '" + docDir.getAbsolutePath() + "' does not exist or is not readable, please check the path");
        throw new IOException("Cannot read from document directory");
      }

      indexDocs(writer, docDir);
      System.out.println("Optimizing...");
      writer.forceMerge(1);
    }
    writer.close();
  }

  /**
   * Index either a file or a directory tree.
   * 
   * @param writer
   * @param file
   * @throws IOException
   */
  void indexDocs(IndexWriter writer, File file) throws IOException {

    /* Strip the absolute part of the path name from file name output */
    int scratchP = scratch.getCanonicalPath().length() + 1;

    /* do not try to index files that cannot be read */
    if (file.canRead()) {
      if (file.isDirectory()) {
        String[] files = file.list();
        // an IO error could occur
        if (files != null) {
          Arrays.sort(files);
          for (int i = 0; i < files.length; i++) {
            indexDocs(writer, new File(file, files[i]));
          }
        }
      } else {
        System.out.println("adding " + file.getCanonicalPath().substring(scratchP));
        try {
          Document doc = new Document();
          FieldType docFT = new FieldType();
          docFT.setTokenized (false);
          docFT.setStored (true);
          docFT.setIndexOptions (IndexOptions.DOCS);

          // Add the path of the file as a field named "path".  Use a field that is
          // indexed (i.e. searchable), but don't tokenize the field into words.
          doc.add(new Field("path", file.getPath(), docFT));

          // Add the last modified date of the file a field named "modified".  Use
          // a field that is indexed (i.e. searchable), but don't tokenize the field
          // into words.
          doc.add(new Field("modified",
                  DateTools.timeToString(file.lastModified(), DateTools.Resolution.MINUTE),
                  docFT));

          // Add the contents of the file to a field named "contents".  Specify a Reader,
          // so that the text of the file is tokenized and indexed, but not stored.
          // Note that FileReader expects the file to be in the system's default encoding.
          // If that's not the case searching for special characters will fail.
          docFT.setTokenized (true);
          docFT.setStored (false);
          doc.add(new Field("contents", new FileReader(file), docFT));
          writer.addDocument(doc);
        }
        // at least on windows, some temporary files raise this exception with
        // an "access denied" message
        // checking if the file can be read doesn't help
        catch (FileNotFoundException fnfe) { }
      }
    }
  }
}
