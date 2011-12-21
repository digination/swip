/* 
/  Copyright (C) 2009  Risto Känsäkoski- Sesca ISW Ltd
/  
/  This file is part of SIP-Applet (www.sesca.com, www.purplescout.com)
/
/  This program is free software; you can redistribute it and/or
/  modify it under the terms of the GNU General Public License
/  as published by the Free Software Foundation; either version 2
/  of the License, or (at your option) any later version.
/
/  This program is distributed in the hope that it will be useful,
/  but WITHOUT ANY WARRANTY; without even the implied warranty of
/  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
/  GNU General Public License for more details.
/
/  You should have received a copy of the GNU General Public License
/  along with this program; if not, write to the Free Software
/  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

package com.sesca.audio;

public class PCMUCodec2 implements AudioTranscoder{
	 int BIAS = 0x84;
	 int cClip = 32635;

	 static int seg_end[] = {0xFF, 0x1FF, 0x3FF, 0x7FF,
		    0xFFF, 0x1FFF, 0x3FFF, 0x7FFF};
	 
	static int MuLawCompressTable[] =
	{
	     0,0,1,1,2,2,2,2,3,3,3,3,3,3,3,3,
	     4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
	     5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
	     5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
	     6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
	     6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
	     6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
	     6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
	     7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
	     7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
	     7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
	     7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
	     7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
	     7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
	     7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
	     7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7
	};
	static int MuLawDecompressTable[] =
	{
	     -32124,-31100,-30076,-29052,-28028,-27004,-25980,-24956,
	     -23932,-22908,-21884,-20860,-19836,-18812,-17788,-16764,
	     -15996,-15484,-14972,-14460,-13948,-13436,-12924,-12412,
	     -11900,-11388,-10876,-10364, -9852, -9340, -8828, -8316,
	      -7932, -7676, -7420, -7164, -6908, -6652, -6396, -6140,
	      -5884, -5628, -5372, -5116, -4860, -4604, -4348, -4092,
	      -3900, -3772, -3644, -3516, -3388, -3260, -3132, -3004,
	      -2876, -2748, -2620, -2492, -2364, -2236, -2108, -1980,
	      -1884, -1820, -1756, -1692, -1628, -1564, -1500, -1436,
	      -1372, -1308, -1244, -1180, -1116, -1052,  -988,  -924,
	       -876,  -844,  -812,  -780,  -748,  -716,  -684,  -652,
	       -620,  -588,  -556,  -524,  -492,  -460,  -428,  -396,
	       -372,  -356,  -340,  -324,  -308,  -292,  -276,  -260,
	       -244,  -228,  -212,  -196,  -180,  -164,  -148,  -132,
	       -120,  -112,  -104,   -96,   -88,   -80,   -72,   -64,
	        -56,   -48,   -40,   -32,   -24,   -16,    -8,     0,
	      32124, 31100, 30076, 29052, 28028, 27004, 25980, 24956,
	      23932, 22908, 21884, 20860, 19836, 18812, 17788, 16764,
	      15996, 15484, 14972, 14460, 13948, 13436, 12924, 12412,
	      11900, 11388, 10876, 10364,  9852,  9340,  8828,  8316,
	       7932,  7676,  7420,  7164,  6908,  6652,  6396,  6140,
	       5884,  5628,  5372,  5116,  4860,  4604,  4348,  4092,
	       3900,  3772,  3644,  3516,  3388,  3260,  3132,  3004,
	       2876,  2748,  2620,  2492,  2364,  2236,  2108,  1980,
	       1884,  1820,  1756,  1692,  1628,  1564,  1500,  1436,
	       1372,  1308,  1244,  1180,  1116,  1052,   988,   924,
	        876,   844,   812,   780,   748,   716,   684,   652,
	        620,   588,   556,   524,   492,   460,   428,   396,
	        372,   356,   340,   324,   308,   292,   276,   260,
	        244,   228,   212,   196,   180,   164,   148,   132,
	        120,   112,   104,    96,    88,    80,    72,    64,
	         56,    48,    40,    32,    24,    16,     8,     0
	}; 	

	public byte[] encode(byte[] frame){
	byte[] encodedFrame=new byte[frame.length/2];
	int j=0;
	for (int i=0;i<frame.length;i+=2)
		{
		int sample = (frame[i+1]<<8)+(frame[i]<<0);

		 // enkoodaus alkaa
		
		int compressedByte=linear2ulaw(sample);
	     
	     // enkoodaus päättyy
	     
	     encodedFrame[j]=(byte)compressedByte;
	     j++;
	} 
	return encodedFrame;
	}
	
	public boolean canEncode() {
		// TODO Auto-generated method stub
		return true;
	}

	public boolean canDecode() {
		// TODO Auto-generated method stub
		return true;
	}

	public boolean supportsSilenceSuppression() {
		// TODO Auto-generated method stub
		return false;
	}


	public byte[] decode(byte[] b) {
		int j=0;
		byte[] decodedFrame=new byte[b.length*2];
		for (int i=0;i<b.length;i++)
		{

			
			int x=b[i]& 0xFF;
			int sample=MuLawDecompressTable[x];
			decodedFrame[j]=(byte) (sample & 0xFF);
			decodedFrame[j+1]=(byte) (int)((sample >> 8) & 0xFF);
			j+=2;
			
			
		}
		return decodedFrame;
	}

	public int getBitRate() {
		// TODO Auto-generated method stub
		return 16;
	}

	public int getSampleRate() {
		// TODO Auto-generated method stub
		return 8000;
	}

	public int getChannels() {
		// TODO Auto-generated method stub
		return 1;
	}

	public int getFrameLength() {
		// TODO Auto-generated method stub
		return 20;
	}

	public int getFrameSize() {
		// TODO Auto-generated method stub
		return 160;
	}

	public int getPayloadType() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void init() {
		// TODO Auto-generated method stub
		
	}
	int	linear2ulaw(
		int		pcm_val)	/* 2's complement (16-bit range) */
	{
		int		mask;
		int		seg;
		int	uval;

		/* Get the sign and the magnitude of the value. */
		if (pcm_val < 0) {
			pcm_val = BIAS - pcm_val;
			mask = 0x7F;
		} else {
			pcm_val += BIAS;
			mask = 0xFF;
		}

		/* Convert the scaled magnitude to segment number. */
		seg = search(pcm_val, seg_end, 8);

		/*
		 * Combine the sign, segment, quantization bits;
		 * and complement the code word.
		 */
		if (seg >= 8)		/* out of range, return maximum value. */
			return (0x7F ^ mask);
		else {
			uval = (seg << 4) | ((pcm_val >> (seg + 3)) & 0xF);
			if (uval==0) uval=0x02;  // Zero trap
			return (uval ^ mask);
		}

	}

	int
	search(
		int		val,
		int[]		table,
		int		size)
	{
		int		i;
		int j=0;
		for (i = 0; i < size; i++) {
			if (val <= table[j++])
				return (i);
		}
		return (size);
	}

	public boolean supportsPseudoEchoCancellation() {
		// TODO Auto-generated method stub
		return false;
	}
	
}
