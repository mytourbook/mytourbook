package net.tourbook.cloud.oauth2;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Source : https://stackoverflow.com/a/54675316/7066681
 */
public class MultiPartBodyPublisher {
   private List<PartsSpecification> partsSpecificationList = new ArrayList<>();
   private String                   boundary               = UUID.randomUUID().toString();

   private class PartsIterator implements Iterator<byte[]> {

      private Iterator<PartsSpecification> iter;
      private InputStream                  currentFileInput;

      private boolean                      done;
      private byte[]                       next;

      PartsIterator() {
         iter = partsSpecificationList.iterator();
      }

      private byte[] computeNext() throws IOException {
         if (currentFileInput == null) {
            if (!iter.hasNext()) {
               return new byte[0];
            }
            final PartsSpecification nextPart = iter.next();
            if (PartsSpecification.TYPE.STRING.equals(nextPart.type)) {
               final String part =
                     "--" + boundary + "\r\n" + //$NON-NLS-1$ //$NON-NLS-2$
                           "Content-Disposition: form-data; name=" + nextPart.name + "\r\n" + //$NON-NLS-1$ //$NON-NLS-2$
                           "Content-Type: text/plain; charset=UTF-8\r\n\r\n" + //$NON-NLS-1$
                           nextPart.value + "\r\n"; //$NON-NLS-1$
               return part.getBytes(StandardCharsets.UTF_8);
            }
            if (PartsSpecification.TYPE.FINAL_BOUNDARY.equals(nextPart.type)) {
               return nextPart.value.getBytes(StandardCharsets.UTF_8);
            }
            String filename;
            String contentType;
            if (PartsSpecification.TYPE.FILE.equals(nextPart.type)) {
               final Path path = nextPart.path;
               filename = path.getFileName().toString();
               contentType = Files.probeContentType(path);
               if (contentType == null) {
                  contentType = "application/octet-stream"; //$NON-NLS-1$
               }
               currentFileInput = Files.newInputStream(path);
            } else {
               filename = nextPart.filename;
               contentType = nextPart.contentType;
               if (contentType == null) {
                  contentType = "application/octet-stream"; //$NON-NLS-1$
               }
               currentFileInput = nextPart.stream.get();
            }
            final String partHeader =
                  "--" + boundary + "\r\n" + //$NON-NLS-1$ //$NON-NLS-2$
                        "Content-Disposition: form-data; name=" + nextPart.name + "; filename=" + filename + "\r\n" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        "Content-Type: " + contentType + "\r\n\r\n"; //$NON-NLS-1$ //$NON-NLS-2$
            return partHeader.getBytes(StandardCharsets.UTF_8);
         } else {
            final byte[] buf = new byte[8192];
            final int r = currentFileInput.read(buf);
            if (r > 0) {
               final byte[] actualBytes = new byte[r];
               System.arraycopy(buf, 0, actualBytes, 0, r);
               return actualBytes;
            } else {
               currentFileInput.close();
               currentFileInput = null;
               return "\r\n".getBytes(StandardCharsets.UTF_8); //$NON-NLS-1$
            }
         }
      }

      @Override
      public boolean hasNext() {
         if (done) {
            return false;
         }
         if (next != null) {
            return true;
         }
         try {
            next = computeNext();
         } catch (final IOException e) {
            throw new UncheckedIOException(e);
         }
         if (next.length == 0) {
            done = true;
            return false;
         }
         return true;
      }

      @Override
      public byte[] next() {
         if (!hasNext()) {
            throw new NoSuchElementException();
         }
         final byte[] res = next;
         next = null;
         return res;
      }
   }

   private static class PartsSpecification {

      PartsSpecification.TYPE type;

      String                  name;
      String                  value;
      Path                    path;
      Supplier<InputStream>   stream;
      String                  filename;
      String                  contentType;

      public enum TYPE {
         STRING, FILE, STREAM, FINAL_BOUNDARY
      }

   }

   private void addFinalBoundaryPart() {
      final PartsSpecification newPart = new PartsSpecification();
      newPart.type = PartsSpecification.TYPE.FINAL_BOUNDARY;
      newPart.value = "--" + boundary + "--"; //$NON-NLS-1$ //$NON-NLS-2$
      partsSpecificationList.add(newPart);
   }

   public MultiPartBodyPublisher addPart(final String name, final Path value) {
      final PartsSpecification newPart = new PartsSpecification();
      newPart.type = PartsSpecification.TYPE.FILE;
      newPart.name = name;
      newPart.path = value;
      partsSpecificationList.add(newPart);
      return this;
   }

   public MultiPartBodyPublisher addPart(final String name, final String value) {
      final PartsSpecification newPart = new PartsSpecification();
      newPart.type = PartsSpecification.TYPE.STRING;
      newPart.name = name;
      newPart.value = value;
      partsSpecificationList.add(newPart);
      return this;
   }

   public HttpRequest.BodyPublisher build() {
      if (partsSpecificationList.size() == 0) {
         throw new IllegalStateException("Must have at least one part to build multipart message."); //$NON-NLS-1$
      }
      addFinalBoundaryPart();
      return HttpRequest.BodyPublishers.ofByteArrays(PartsIterator::new);
   }

   public String getBoundary() {
      return boundary;
   }
}
