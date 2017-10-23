package io.scif.formats;

import io.scif.AbstractChecker;
import io.scif.AbstractFormat;
import io.scif.AbstractMetadata;
import io.scif.AbstractParser;
import io.scif.ByteArrayPlane;
import io.scif.ByteArrayReader;
import io.scif.Format;
import io.scif.FormatException;
import io.scif.ImageMetadata;
import io.scif.config.SCIFIOConfig;
import io.scif.io.ByteArrayHandle;
import io.scif.io.RandomAccessInputStream;
import io.scif.io.RandomAccessOutputStream;
import io.scif.util.FormatTools;

import java.io.IOException;

import net.imagej.axis.Axes;

import org.scijava.plugin.Plugin;


@Plugin(type = Format.class, name = "Raw 10-bit multi frame")
public class Raw10Format_ extends AbstractFormat {

	@Override
	protected String[] makeSuffixArray() {
		return new String[] { "r10" };
	}

	public static class Metadata extends AbstractMetadata {

		@Override
		public void populateImageMetadata() {
			final ImageMetadata m = get(0);
			m.setPlanarAxisCount(2);
			m.setAxisTypes(Axes.X, Axes.Y, Axes.CHANNEL, Axes.TIME);
			m.setIndexed(false);
			m.setFalseColor(false);
			m.setPixelType(FormatTools.UINT8);
			m.setMetadataComplete(true);
		}

	/*	@Override
		public void close(final boolean fileOnly) throws IOException {
			super.close(fileOnly);
			}
		} */
	}

	public static class Checker extends AbstractChecker {

		public static final int RAW10_MAGIC = 0x10101010;

		@Override
		public boolean suffixNecessary() {
			return false;
		}

		@Override
		public boolean isFormat(final RandomAccessInputStream stream)
			throws IOException
		{
			if (!FormatTools.validStream(stream, 4, false)) return false;
			return stream.readInt() == RAW10_MAGIC;
		}

	}

	public static class Parser extends AbstractParser<Metadata> {

		@Override
		protected void typedParse(final RandomAccessInputStream stream,
			final Metadata meta, final SCIFIOConfig config) throws IOException,
			FormatException
		{
			int magic, nframe, height, width;
			stream.order(true);
			magic = stream.readInt();
			nframe= stream.readInt();
			height= stream.readInt();
			width = stream.readInt();
			if (magic != 0x10101010) throw new FormatException("Invalid magic " + magic);
			if (nframe < 0 || nframe >  1023 ||
			    height < 0 || height > 65535 ||
			    width  < 0 || width  > 65535)
			    throw new FormatException("Invalid dimensions " + nframe + "," + height + "," + width);
			meta.createImageMetadata(1);
			final ImageMetadata iMeta = meta.get(0);
			iMeta.setAxisLength(Axes.X, width);
			iMeta.setAxisLength(Axes.Y, height);
			iMeta.setAxisLength(Axes.CHANNEL, 1);
			iMeta.setAxisLength(Axes.TIME, nframe);
		}
	}

	public static class Reader extends ByteArrayReader<Metadata> {

		@Override
		protected String[] createDomainArray() {
			return new String[] { FormatTools.GRAPHICS_DOMAIN };
		}

		@Override
		public ByteArrayPlane openPlane(final int imageIndex,
			final long planeIndex, final ByteArrayPlane plane, final long[] planeMin,
			final long[] planeMax, final SCIFIOConfig config) throws FormatException,
			IOException
		{
			//FormatTools.checkPlaneForReading(meta, imageIndex, planeIndex, out.length, planeMin, planeMax);
			//check file length == 16 + nframe * height * (width + (width >> 2))
			final RandomAccessInputStream s = getStream();
			final ImageMetadata meta = getMetadata().get(0);
			final int width = (int)meta.getAxisLength(Axes.X),
					  height= (int)meta.getAxisLength(Axes.Y);
			final int width_bytes = width + (width >> 2); //10-bits / 8 bits/byte
			s.seek(16 + planeIndex * height * width_bytes);
			final byte[] out = plane.getData();
			final byte[] buf = new byte[width_bytes];
			for (int i=0; i < height * width; ) {
				s.read(buf);
				for (int j=0; j < width_bytes; j+=5, i+=4) {
					//2:10, 12:20, 22:30, 32:40
					out[i+0] = (byte)((buf[j+0] >> 2) | ((buf[j+1] << 6) & 255));
					out[i+1] = (byte)((buf[j+1] >> 4) | ((buf[j+2] << 4) & 255));
					out[i+2] = (byte)((buf[j+2] >> 6) | ((buf[j+3] << 2) & 255));
					out[i+3] = buf[j+4];
				}
			}
			return plane;
		}
	}
}
