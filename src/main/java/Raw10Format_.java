//package io.scif.formats;

import io.scif.AbstractChecker;
import io.scif.AbstractFormat;
import io.scif.AbstractMetadata;
import io.scif.AbstractParser;
import io.scif.ByteArrayPlane;
import io.scif.ByteArrayReader;
//import io.scif.DataPlane;
//import io.scif.TypedReader;
//import io.scif.AbstractReader;
import io.scif.Format;
import io.scif.FormatException;
import io.scif.ImageMetadata;
import io.scif.config.SCIFIOConfig;
//import io.scif.io.ByteArrayHandle;
import io.scif.io.RandomAccessInputStream;
//import io.scif.io.RandomAccessOutputStream;
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
			m.setPixelType(FormatTools.UINT16); //8);
			m.setMetadataComplete(true);
			m.setLittleEndian(true);
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
	//AbstractReader<Metadata, DataPlane<short>> {
	//TypedReader<Metadata, DataPlane<short>> {

		@Override
		protected String[] createDomainArray() {
			return new String[] { FormatTools.GRAPHICS_DOMAIN };
		}
/*
		@Override
		public DataPlane<short> openThumbPlane(final int imageIndex,
			final long planeIndex) throws FormatException, IOException
		{
			FormatTools.assertStream(getStream(), true, 1);
			final Metadata meta = getMetadata();
			final long[] planeBounds = meta.get(imageIndex).getAxesLengthsPlanar();
			final long[] planeOffsets = new long[planeBounds.length];

			planeBounds[meta.get(imageIndex).getAxisIndex(Axes.X)] =
				meta.get(imageIndex).getThumbSizeX();
			planeBounds[meta.get(imageIndex).getAxisIndex(Axes.Y)] =
				meta.get(imageIndex).getThumbSizeX();

			final ByteArrayPlane plane = createPlane(planeOffsets, planeBounds);

			plane.setData(FormatTools.openThumbBytes(this, imageIndex, planeIndex));

			return plane;
		}

		@Override
		public ByteArrayPlane createPlane(final long[] planeOffsets,
			final long[] planeBounds)
		{
			return createPlane(getMetadata().get(0), planeOffsets, planeBounds);
		}

		@Override
		public ByteArrayPlane createPlane(final ImageMetadata meta,
			final long[] planeOffsets, final long[] planeBounds)
		{
			return new DataPlane<short>(getContext(), meta, planeOffsets, planeBounds);
		}
*/
		@Override
		public ByteArrayPlane/*DataPlane<short>*/ openPlane(final int imageIndex,
			final long planeIndex, final ByteArrayPlane/*DataPlane<short>*/ plane, final long[] planeMin,
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
		/*	final ByteArrayHandle handle = new ByteArrayHandle(height*width*2);
			final RandomAccessOutputStream out = new RandomAccessOutputStream(handle);
			out.order(true); //little endian
		*/	final byte[] out = plane.getData();
			//final short[] out = plane.getData();
			final byte[] buf = new byte[width_bytes];
		/*	for (int i=0; i < height * width; ) {
				s.read(buf);
				for (int j=0; j < width_bytes; j+=5, i+=4) {
					//0:10, 10:20, 20:30, 30:40 -> 6:16
					int x0=buf[j+0], x1=buf[j+1], x2=buf[j+2], x3=buf[j+3], x4=buf[j+4];
					out[i+0] = (short)(((x0 & 0xFF) << 6) | ((x1 & 0x03) << 14));
					out[i+1] = (short)(((x1 & 0xFC) << 4) | ((x2 & 0x0F) << 12));
					out[i+2] = (short)(((x2 & 0xF0) << 2) | ((x3 & 0x3F) << 10));
					out[i+3] = (short)(( x3 & 0xC0      ) | ((x4 & 0xFF) <<  8));
			*/
			for (int i=0; i < height * width * 2; ) {
				s.read(buf);
				for (int j=0; j < width_bytes; j+=5, i+=8) {
					//0:8, 8:10, 10:16 | 16:18, 18:20, 20:24 | 24:28, 28:30, 30:32 | 32:38, 38:40 
					out[i+0] =          buf[j+0];
					out[i+1] = (byte)(  buf[j+1] & 0x03);
					out[i+2] = (byte)(((buf[j+1] & 0xFC) >> 2) | ((buf[j+2] & 0x03) << 6));
					out[i+3] = (byte)( (buf[j+2] & 0x0C) >> 2);
					out[i+4] = (byte)(((buf[j+2] & 0xF0) >> 4) | ((buf[j+3] & 0x0F) << 4));
					out[i+5] = (byte)( (buf[j+3] & 0x30) >> 4);
					out[i+6] = (byte)(((buf[j+3] & 0xC0) >> 6) | ((buf[j+4] & 0x3F) << 2));
					out[i+7] = (byte)( (buf[j+4] & 0xC0) >> 6);
				}
			}
		/*	out.close();
			final RandomAccessInputStream s = new RandomAccessInputStream(getContext(), handle);
			s.seek(0);
			readPlane(s, imageIndex, planeMin, planeMax, plane);
			s.close();  */
			return plane;
		}
	}
}
