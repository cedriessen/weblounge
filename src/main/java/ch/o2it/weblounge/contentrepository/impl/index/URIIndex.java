/*
 *  Weblounge: Web Content Management System
 *  Copyright (c) 2010 The Weblounge Team
 *  http://weblounge.o2it.ch
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software Foundation
 *  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package ch.o2it.weblounge.contentrepository.impl.index;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * This index is part of the content repository index and maps page identifiers
 * to paths. In order to quickly get to an entry, use the <code>id</code> or
 * <code>path</code> index.
 * 
 * <pre>
 * | slot | id      | path
 * |------------------------------------------
 * | 1    | a-b-c-d | /etc/weblounge
 * </pre>
 */
public class URIIndex {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(URIIndex.class);

  /** Default number of bytes used per id */
  private static final int IDX_BYTES_PER_ID = 36;

  /** Default number of bytes per path */
  private static final int IDX_BYTES_PER_PATH = 128;

  /** Location of the bytes-per-id header */
  protected static final long IDX_BYTES_PER_ID_HEADER_LOCATION = 0;

  /** Location of the bytes-per-path header */
  protected static final long IDX_BYTES_PER_PATH_HEADER_LOCATION = 4;

  /** Location of the entries header */
  protected static final long IDX_SLOTS_HEADER_LOCATION = 8;

  /** Location of the entries header */
  protected static final long IDX_ENTRIES_HEADER_LOCATION = 16;

  /** Number of bytes that are used for the index header */
  protected static final int IDX_HEADER_SIZE = 24;

  /** Location of the first entry */
  protected static final long IDX_ENTRIES_LOCATION = IDX_HEADER_SIZE;

  /** The index file */
  protected RandomAccessFile idx = null;

  /** The index file */
  protected File idxFile = null;

  /** True if this is a readonly index */
  protected boolean isReadOnly = false;

  /** Number of bytes per id */
  protected int bytesPerId = IDX_BYTES_PER_ID;

  /** Number of bytes per path */
  protected int bytesPerPath = IDX_BYTES_PER_PATH;

  /** Number of bytes per entry */
  protected int bytesPerEntry = bytesPerId + bytesPerPath;

  /** Number of entries */
  protected long entries = 0;

  /** Number of slots (entries + empty space) */
  protected long slots = 0;

  /**
   * Creates an index from the given file. If the file does not exist, it is
   * created and initialized with the default index settings, which means that
   * uri identifiers are expected to be made out of 36 bytes (uuid) while paths
   * are allowed up to 128 bytes.
   * <p>
   * Note that the path length will automatically be increased as soon as longer
   * paths are added, while the size of identifiers is fixed.
   * 
   * @param indexFile
   *          location of the index file
   * @param readOnly
   *          <code>true</code> to indicate a read only index
   * @throws IOException
   *           if reading from the index fails
   */
  public URIIndex(File indexFile, boolean readOnly) throws IOException {
    this(indexFile, readOnly, IDX_BYTES_PER_ID, IDX_BYTES_PER_PATH);
  }

  /**
   * Creates an index from the given file. If the file does not exist, it is
   * created and initialized with the default index settings, which means that
   * uri identifiers are expected to be made out of 36 bytes (uuid).
   * <p>
   * Note that the path length will automatically be increased as soon as longer
   * paths are added, while the size of identifiers is fixed.
   * 
   * @param indexFile
   *          location of the index file
   * @param pathLengthInBytes
   *          the number of bytes per path
   * @param readOnly
   *          <code>true</code> to indicate a read only index
   * @throws IOException
   *           if reading from the index fails
   */
  public URIIndex(File indexFile, boolean readOnly, int pathLengthInBytes)
      throws IOException {
    this(indexFile, readOnly, IDX_BYTES_PER_ID, pathLengthInBytes);
  }

  /**
   * Creates an index from the given file. If the file does not exist, it is
   * created and initialized with the default index settings.
   * <p>
   * The number of bytes per entry defines the size of the index.
   * 
   * @param indexFile
   *          location of the index file
   * @param readOnly
   *          <code>true</code> to indicate a read only index
   * @param idLengthInBytes
   *          the number of bytes per id
   * @param pathLengthInBytes
   *          the number of bytes per path
   * @throws IOException
   *           if reading from the index fails
   */
  public URIIndex(File indexFile, boolean readOnly, int idLengthInBytes,
      int pathLengthInBytes) throws IOException {

    this.idxFile = indexFile;
    this.isReadOnly = readOnly;

    String mode = readOnly ? "r" : "rwd";
    try {
      idx = new RandomAccessFile(indexFile, mode);
    } catch (FileNotFoundException e) {
      throw new IllegalArgumentException("Index file " + indexFile + " does not exist");
    }

    // Read index header information
    try {
      this.bytesPerPath = idx.readInt();
      this.entries = idx.readLong();
      this.bytesPerEntry = IDX_BYTES_PER_ID + pathLengthInBytes;
      if (this.bytesPerId != idLengthInBytes || this.bytesPerPath != pathLengthInBytes)
        resize(idLengthInBytes, pathLengthInBytes);
    } catch (EOFException e) {
      if (readOnly) {
        throw new IllegalStateException("Readonly index cannot be empty");
      }
      logger.info("Initializing index with default index values");
      init(idLengthInBytes, pathLengthInBytes);
    } catch (IOException e) {
      logger.error("Error reading from path index: " + e.getMessage());
      throw e;
    }
  }

  /**
   * Closes this index.
   * 
   * @throws IOException
   *           if closing the index file fails
   */
  public void close() throws IOException {
    idx.close();
  }

  /**
   * Returns the index size in bytes. The size is calculated from the size of
   * the header plus the number of slots multiplied by the size of one slot.
   * 
   * @return the index size
   */
  public long size() {
    return IDX_HEADER_SIZE + (slots * bytesPerEntry);
  }

  /**
   * Returns the number of bytes per entry;
   * 
   * @return the number of bytes per entry
   */
  public int getEntrySize() {
    return bytesPerPath;
  }

  /**
   * Returns the number of entries.
   * 
   * @return the number of entries
   */
  public long getEntries() {
    return entries;
  }

  /**
   * Adds id and path to the index and returns the index address.
   * 
   * @param id
   *          the identifier
   * @param path
   *          the page path
   * @return the entry's address in this index
   * @throws IOException
   *           if writing to the index fails
   */
  public synchronized long add(String id, String path) throws IOException {
    if (id.getBytes().length != bytesPerId)
      throw new IllegalArgumentException(bytesPerId + " byte identifier required");

    long entry = entries;
    long startOfEntry = IDX_HEADER_SIZE + (entries * bytesPerEntry);

    int pathLengthInBytes = path.getBytes().length;

    // Make sure there is still room left for an additional entry. One entry
    // consists of the uuid, the path and a closing '\n'
    if (pathLengthInBytes >= bytesPerPath) {
      logger.info("Path doesn't fit, triggering index resize");
      int newBytesPerPath = bytesPerPath * 2;
      while (newBytesPerPath < pathLengthInBytes)
        newBytesPerPath *= 2;
      resize(bytesPerId, newBytesPerPath);
      startOfEntry = IDX_HEADER_SIZE + (entries * bytesPerEntry);
    }

    // See if there is an empty slot
    long address = IDX_HEADER_SIZE;
    long e = 0;
    boolean reusingSlot = false;
    idx.seek(address);
    while (address < startOfEntry) {
      if (idx.readChar() == '\n') {
        logger.debug("Found orphan line for reuse");
        startOfEntry = address;
        reusingSlot = true;
        entry = e;
        break;
      }
      idx.skipBytes(bytesPerEntry - 2);
      address += bytesPerEntry;
      e++;
    }

    // Add the new address at the end
    idx.seek(startOfEntry);
    idx.write(id.getBytes());
    idx.write(path.getBytes());
    idx.write('\n');
    long remainingBytes = bytesPerEntry - IDX_BYTES_PER_ID - pathLengthInBytes - 1;
    for (int i = 0; i < remainingBytes; i++) {
      idx.writeByte(0);
    }

    if (!reusingSlot)
      slots++;
    entries++;

    // Update the file header
    idx.seek(IDX_SLOTS_HEADER_LOCATION);
    idx.writeLong(slots);
    idx.writeLong(entries);

    logger.debug("Added uri with id '{}' and path '{}' as entry no {}", new Object[] {
        id,
        path,
        entries });
    return entry;
  }

  /**
   * Removes all entries for the given page uri from the index.
   * 
   * @param entry
   *          start address of uri entry
   * @throws IOException
   *           if removing the entry from the index fails
   */
  public synchronized void delete(long entry) throws IOException {
    long startOfEntry = IDX_HEADER_SIZE + (entry * bytesPerEntry);

    // Remove the entry by writing a '\n' to the first byte
    idx.seek(startOfEntry);
    idx.write('\n');
    for (int i = 1; i < bytesPerEntry; i++) {
      idx.writeLong(0);
    }

    // Update the file header
    entries--;
    idx.seek(IDX_ENTRIES_HEADER_LOCATION);
    idx.writeLong(entries);

    logger.debug("Removed uri at address '{}' from index", entry);
  }

  /**
   * Updates the path on the uri located at <code>entry</code>.
   * 
   * @param entry
   *          start address of uri
   * @param path
   *          the new path
   * @throws IOException
   *           if updating the path in the index fails
   */
  public synchronized void update(long entry, String path) throws IOException {
    long startOfEntry = IDX_HEADER_SIZE + (entry * bytesPerEntry);

    // Check if the new path fits the current index
    int pathLengthInBytes = path.getBytes().length;
    if (pathLengthInBytes >= bytesPerPath) {
      logger.info("Path doesn't fit, triggering index resize");
      int newBytesPerPath = bytesPerPath * 2;
      while (newBytesPerPath < pathLengthInBytes)
        newBytesPerPath *= 2;
      resize(bytesPerId, newBytesPerPath);
      startOfEntry = IDX_HEADER_SIZE + (entry * bytesPerEntry);
    }

    // Write the path to the index
    idx.seek(startOfEntry);
    idx.skipBytes(bytesPerId);
    idx.write(path.getBytes());
    idx.write('\n');
    for (int i = 1; i < bytesPerEntry; i++) {
      idx.writeLong(0);
    }

    logger.debug("Updated uri at address '{}' to {}", entry, path);
  }

  /**
   * Removes all entries from the index.
   * 
   * @throws IOException
   *           if writing to the index fails
   */
  public synchronized void clear() throws IOException {
    init(bytesPerId, bytesPerPath);
  }

  /**
   * Returns the uri's id.
   * 
   * @param entry
   *          the entry at which the id is expected
   * @return the id
   * @throws IllegalStateException
   *           if the user tries to access an address with no data
   * @throws EOFException
   *           if the user tries to access a non-existing address
   * @throws IOException
   *           if reading from the index fails
   */
  public synchronized String getId(long entry) throws IOException, EOFException {
    long startOfEntry = IDX_HEADER_SIZE + (entry * bytesPerEntry);
    idx.seek(startOfEntry);
    byte[] bytes = new byte[bytesPerId];
    int bytesRead = idx.read(bytes);
    if (bytesRead < bytesPerId || bytes[0] == '\n')
      throw new IllegalStateException("No data at address " + entry);
    return new String(bytes);
  }

  /**
   * Returns the uri's path.
   * 
   * @param entry
   *          the entry at which the id is expected
   * @return the path
   * @throws EOFException
   *           if the user tries to access a non-existing address
   * @throws IOException
   *           if reading from the index fails
   */
  public synchronized String getPath(long entry) throws IOException,
      EOFException {
    long startOfEntry = IDX_HEADER_SIZE + (entry * bytesPerEntry);
    idx.seek(startOfEntry);
    idx.skipBytes(bytesPerId);

    byte[] bytes = new byte[bytesPerPath];
    idx.read(bytes);
    for (int i = 0; i < bytes.length; i++) {
      if (bytes[i] == '\n')
        return new String(bytes, 0, i);
    }

    throw new IllegalStateException("Found path without delimiter");
  }

  /**
   * Initializes the index file to the given size using <code>0</code> byte
   * values and writes the header information to it.
   * 
   * @param bytesPerEntry
   *          the number of bytes per entry
   * @throws IOException
   *           writing to the index fails
   */
  private void init(int bytesPerId, int bytesPerPath) throws IOException {
    this.bytesPerId = bytesPerId;
    this.bytesPerPath = bytesPerPath;
    this.bytesPerEntry = bytesPerId + bytesPerPath;

    // Write header
    idx.seek(0);
    idx.writeInt(bytesPerId);
    idx.writeInt(bytesPerPath);
    idx.writeLong(0);
    idx.writeLong(0);

    // If this file used to contain entries, we just null out the rest
    try {
      while (idx.getFilePointer() < idx.length()) {
        idx.write('\n');
        for (int j = 1; j < bytesPerEntry; j++)
          idx.write(0);
      }
    } catch (EOFException e) {
      // That's ok, we wanted to write until the very end
    }

    this.entries = 0;

    logger.info("Uri index initialized with {} bytes per entry", bytesPerEntry);
  }

  /**
   * Resizes the index file to the given number of bytes per entry.
   * 
   * @param newBytesPerId
   *          the number of bytes per id
   * @param newBytesPerPath
   *          the number of bytes per entry
   * @throws IOException
   *           writing to the index fails
   * @throws IllegalStateException
   *           if the index is read only or if the user tries to resize the
   *           number of slots while there are already entries in the index
   */
  public synchronized void resize(int newBytesPerId, int newBytesPerPath)
      throws IOException {
    if (this.bytesPerId > newBytesPerId && this.entries > 0)
      throw new IllegalStateException("Cannot reduce the number of bytes per id when there are entries in the index");
    if (this.bytesPerPath > newBytesPerPath && this.entries > 0)
      throw new IllegalStateException("Cannot reduce the number of bytes per path when there are entries in the index");
    if (this.isReadOnly)
      throw new IllegalStateException("This index is readonly");

    int newBytesPerEntry = newBytesPerId + newBytesPerPath;

    logger.info("Resizing uri index with {} entries to {} bytes per entry", entries, newBytesPerEntry);

    String idxFilename = idxFile.getName();
    String fileName = FilenameUtils.getBaseName(idxFilename);
    String fileExtension = FilenameUtils.getExtension(idxFilename);
    String idxFilenameNew = fileName + "_resized." + fileExtension;
    File newIdxFile = new File(idxFile.getParentFile(), idxFilenameNew);
    long time = System.currentTimeMillis();

    logger.debug("Creating resized index at " + newIdxFile);

    RandomAccessFile idxNew = null;
    try {
      idxNew = new RandomAccessFile(newIdxFile, "rwd");
    } catch (FileNotFoundException e) {
      throw new IllegalArgumentException("Index file " + newIdxFile + " cannot be created: " + e.getMessage(), e);
    }

    // Write header
    idxNew.seek(0);
    idxNew.writeInt(newBytesPerId);
    idxNew.writeInt(newBytesPerPath);
    idxNew.writeLong(slots);
    idxNew.writeLong(entries);

    // Copy the current index to the new one
    idx.seek(IDX_ENTRIES_LOCATION);
    for (int i = 0; i < this.entries; i++) {
      byte[] bytes = new byte[this.bytesPerEntry];
      idx.read(bytes);
      idxNew.write(bytes);
      for (int j = this.bytesPerEntry; j < newBytesPerEntry; j++)
        idxNew.write(0);
    }

    idxNew.close();

    logger.debug("Removing old index at " + idxFile);
    idxFile.delete();
    logger.debug("Moving resized index into regular position at " + idxFile);
    newIdxFile.renameTo(idxFile);

    try {
      idx = new RandomAccessFile(idxFile, "rwd");
    } catch (FileNotFoundException e) {
      throw new IllegalArgumentException("Index file " + newIdxFile + " cannot be created: " + e.getMessage(), e);
    }

    this.bytesPerEntry = newBytesPerEntry;
    this.bytesPerId = newBytesPerId;
    this.bytesPerPath = newBytesPerPath;
    this.idxFile = newIdxFile;

    time = System.currentTimeMillis() - time;
    logger.info("Uri index resized in {} ms", time);
  }

}
