/*
 * Copyright (c) 2006-2008, IPD Boehm, Universitaet Karlsruhe (TH)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Universität Karlsruhe (TH) nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.uka.ipd.idaho.plugins.spellChecking;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

/**
 * @author sautter
 *
 */
public class LineExtractorTest {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		File imageFile = new File("E:/Projektdaten/test_staple/5834_Page_2.jpg");
//		File imageFile = new File("E:/Projektdaten/Zootaxa_staple/21825_Seite_02.jpg");
		
		InputStream is = new FileInputStream(imageFile);
		BufferedImage image = ImageIO.read(is);
		is.close();
		System.out.println("   - image loaded, color model is " + image.getColorModel().getClass().getName());
		System.out.println("   - image type is " + image.getType());
		
		image = cutImage(image);
		System.out.println("   - image truncated, color model is " + image.getColorModel().getClass().getName());
		
		image = cloneImage(image);
		System.out.println("   - image cloned, type is " + image.getType());
		
		int width = image.getWidth();
		int height = image.getHeight();
		System.out.println("     - size is " + width + " x " + height);
		
		float[] colBrightnesses = new float[width];
		for (int w = 0; w < width; w++) {
			float brightnessSum = 0f; 
			float[] hsb = null;
			int rgb;
			for (int y = 0; y < height; y++) {
				rgb = image.getRGB(w, y);
				hsb = new Color(rgb).getColorComponents(hsb);
				brightnessSum += hsb[2];
			}
			colBrightnesses[w] = (brightnessSum / height);
		}
		
		float[] rowBrightnesses = new float[height];
		for (int h = 0; h < height; h++) {
			float brightnessSum = 0f; 
			float[] hsb = null;
			int rgb;
			for (int x = 0; x < width; x++) {
				rgb = image.getRGB(x, h);
				hsb = new Color(rgb).getColorComponents(hsb);
				brightnessSum += hsb[2];
			}
			rowBrightnesses[h] = (brightnessSum / width);
		}
		
		int offset = 100; // don't use the outmost element
		boolean useMinMax = true;
		
		float[] cutRowBrightnesses = new float[height];
		
		Color color = Color.RED;
		System.out.println("Color: " + color.getRGB());
		int colorRgb = color.getRGB(); //(color.getRGB() & ((1 << 24) - 1));
		System.out.println("ColorRGB: " + colorRgb);
		
		boolean[] rowPainted = new boolean[height];
		
		if (useMinMax) {
			
			float minColBrightness = getTth(colBrightnesses, offset, false);
			float maxColBrightness = getTth(colBrightnesses, offset, true);
			System.out.println("ColBrightness: " + minColBrightness + " - " + maxColBrightness);
			
			//	find to-paint cols
			int minCol = -1;
			int maxCol = width;
			for (int w = 0; w < width; w++) {
				float colBrightness = colBrightnesses[w];
				float minDiff = Math.abs(colBrightness - minColBrightness);
				float maxDiff = Math.abs(colBrightness - maxColBrightness);
				
				if (!paint(minDiff, maxDiff)) {
					if (minCol == -1)
						minCol = w;
					maxCol = w;
				}
			}
			
			//	re-compute rows
			for (int h = 0; h < height; h++) {
				float brightnessSum = 0f; 
				float[] hsb = null;
				int rgb;
				for (int x = minCol; x <= maxCol; x++) {
					rgb = image.getRGB(x, h);
					hsb = new Color(rgb).getColorComponents(hsb);
					brightnessSum += hsb[2];
				}
				cutRowBrightnesses[h] = (brightnessSum / (maxCol - minCol + 1));
			}
			
			//	paint cols
			for (int w = 0; w < width; w++) {
				float colBrightness = colBrightnesses[w];
				float minDiff = Math.abs(colBrightness - minColBrightness);
				float maxDiff = Math.abs(colBrightness - maxColBrightness);
				
				if (paint(minDiff, maxDiff)) {
					for (int y = 0; y < height; y++)
						paint(w, y, colorRgb, image, true);
				}
			}
			
//			float minRowBrightness = getTth(rowBrightnesses, offset, false);
//			float maxRowBrightness = getTth(rowBrightnesses, offset, true);
			float minRowBrightness = getTth(cutRowBrightnesses, offset, false);
			float maxRowBrightness = getTth(cutRowBrightnesses, offset, true);
			System.out.println("RowBrightness: " + minRowBrightness + " - " + maxRowBrightness);
			
			//	paint rows
			for (int h = 0; h < height; h++) {
//				float rowBrightness = rowBrightnesses[h];
				float rowBrightness = cutRowBrightnesses[h];
				float minDiff = Math.abs(rowBrightness - minRowBrightness);
				float maxDiff = Math.abs(rowBrightness - maxRowBrightness);
				
				if (paint(minDiff, maxDiff)) {
					for (int x = 0; x < width; x++)
						paint(x, h, colorRgb, image, true);
					rowPainted[h] = true;
				}
			}
		}
		else {
			
			float colBrightnessPivot = getPivot(colBrightnesses, offset);
			System.out.println("ColBrightness: " + colBrightnessPivot);
			
			//	find to-paint cols
			int minCol = -1;
			int maxCol = width;
			for (int w = 0; w < width; w++) {
				if (colBrightnesses[w] <= colBrightnessPivot) {
					if (minCol == -1)
						minCol = w;
					maxCol = w;
				}
			}
			
			//	re-compute rows
			for (int h = 0; h < height; h++) {
				float brightnessSum = 0f; 
				float[] hsb = null;
				int rgb;
				for (int x = minCol; x <= maxCol; x++) {
					rgb = image.getRGB(x, h);
					hsb = new Color(rgb).getColorComponents(hsb);
					brightnessSum += hsb[2];
				}
				cutRowBrightnesses[h] = (brightnessSum / (maxCol - minCol + 1));
			}
			
			//	paint cols
			for (int w = 0; w < width; w++) {
				if (colBrightnesses[w] > colBrightnessPivot) {
					for (int y = 0; y < height; y++)
						paint(w, y, colorRgb, image, true);
				}
			}
			
//			float rowBrightnessPivot = getPivot(rowBrightnesses, offset);
			float rowBrightnessPivot = getPivot(cutRowBrightnesses, offset);
			System.out.println("RowBrightness: " + rowBrightnessPivot);
			
			//	paint rows
			for (int h = 0; h < height; h++) {
				if (cutRowBrightnesses[h] > rowBrightnessPivot) {
					for (int x = 0; x < width; x++)
						paint(x, h, colorRgb, image, true);
					rowPainted[h] = true;
				}
			}
		}
		
		//	find bundles of painted rows
		int painted = 0;
		int[] paintBuckets = new int[height];
		for (int h = 0; h < height; h++) {
			if (rowPainted[h])
				painted++;
			else if (painted != 0) {
				paintBuckets[painted]++;
				painted = 0;
			}
		}
		
		//	find most frequent bundle size (likely line spacing)
		int maxPainted = paintBuckets.length;
		while ((maxPainted != 0) && (paintBuckets[maxPainted-1] == 0))
			maxPainted--;
		System.out.println("MaxPainted: " + maxPainted);
		for (int p = 0; p < maxPainted; p++)
			System.out.print(paintBuckets[p] + " - ");
		int maxDiff = 0;
		int lineSpacingHeight = height;
		for (int i = 1; i < paintBuckets.length; i++) {
			int diff = Math.abs(paintBuckets[i] - paintBuckets[i-1]);
			if (maxDiff < diff) {
				maxDiff = diff;
				lineSpacingHeight = i;
			}
		}
		System.out.println("-> choose " + lineSpacingHeight);
		lineSpacingHeight = (lineSpacingHeight / 2);
		
		//	cut bundles of painted rows at brightest painted row closest to middle
		int cutRgb = Color.BLUE.getRGB();
		int lastCut = 0;
		int[] cutDistBuckets = new int[height];
		boolean[] rowCut = new boolean[height];
		
		//	add cut at top of page
		for (int h = 0; h < height; h++) {
			if (!rowPainted[h]) {
				float maxRowBrightness = 0f;
				int blockStart = 0;
				int blockEnd = h;
				for (int b = blockStart; b < blockEnd; b++) {
					if (maxRowBrightness < cutRowBrightnesses[b])
						maxRowBrightness = cutRowBrightnesses[b];
				}
				while (cutRowBrightnesses[blockEnd - 1] < maxRowBrightness)
					blockEnd--;
				
				for (int b = blockStart; b < blockEnd; b++) {
					for (int x = 0; x < width; x++)
						paint(x, b, cutRgb, image, false);
					rowCut[b] = true;
				}
				lastCut = (blockEnd - 1);
				h = height;
			}
		}
		
		//	find cuts in page
		painted = 0;
		for (int h = lastCut; h < height; h++) {
			if (rowPainted[h])
				painted++;
			else {
				if (painted > lineSpacingHeight) {
					float maxRowBrightness = 0f;
					int blockStart = h-painted;
					int blockEnd = h;
					for (int b = blockStart; b < blockEnd; b++) {
						if (maxRowBrightness < cutRowBrightnesses[b])
							maxRowBrightness = cutRowBrightnesses[b];
					}
					while (cutRowBrightnesses[blockStart] < maxRowBrightness)
						blockStart++;
					while (cutRowBrightnesses[blockEnd - 1] < maxRowBrightness)
						blockEnd--;
					
					cutDistBuckets[blockStart - lastCut]++;
					
					for (int b = blockStart; b < blockEnd; b++) {
						for (int x = 0; x < width; x++)
							paint(x, b, cutRgb, image, false);
						rowCut[b] = true;
					}
					lastCut = (blockEnd - 1);
				}
				painted = 0;
			}
		}
		
		//	add cut at bottom of page
		for (int h = (height - 1); h >= 0; h--) {
			if (!rowPainted[h]) {
				float maxRowBrightness = 0f;
				int blockStart = h+1;
				int blockEnd = height;
				for (int b = blockStart; b < blockEnd; b++) {
					if (maxRowBrightness < cutRowBrightnesses[b])
						maxRowBrightness = cutRowBrightnesses[b];
				}
				while ((blockStart < height) && (cutRowBrightnesses[blockStart] < maxRowBrightness))
					blockStart++;
				
				cutDistBuckets[blockStart - lastCut]++;
				
				for (int b = blockStart; b < blockEnd; b++) {
					for (int x = 0; x < width; x++)
						paint(x, b, cutRgb, image, false);
					rowCut[b] = true;
				}
				h = -1;
			}
		}
		
		//	compute most frequent cut distances (likely line hight)
		int maxCutDist = cutDistBuckets.length;
		while ((maxCutDist != 0) && (cutDistBuckets[maxCutDist-1] == 0))
			maxCutDist--;
		System.out.println("MaxCutDist: " + maxCutDist);
		for (int c = 0; c < maxCutDist; c++)
			System.out.print(cutDistBuckets[c] + " - ");
		System.out.println("-> choose TODO");
		
		//	get most frequent cut distance
		int lineCutDist = findBulkCenter(cutDistBuckets, 5);
		System.out.println("LineCutDist: " + lineCutDist);
		
		
		//	find painted rows that do not contain a cut, but are in line height distance to cuts
		painted = 0;
		for (int h = lastCut; h < height; h++) {
			if (rowPainted[h])
				painted++;
			else if (painted != 0) {
				int blockStart = h-painted;
				int blockEnd = h;
				boolean isCut = false;
				for (int b = blockStart; b < blockEnd; b++)
					if (rowCut[b]) isCut = true;
				
				if (!isCut) {
					int lastRowCut = blockStart;
					while (!rowCut[lastRowCut])
						lastRowCut--;
					int nextRowCut = blockEnd;
					while (!rowCut[nextRowCut])
						nextRowCut++;
					
					boolean doCut = false;
					for (int b = blockStart; b < blockEnd; b++) {
						if (Math.abs(Math.abs(b - lastRowCut) - lineCutDist) < lineSpacingHeight)
							doCut = true;
						else if (Math.abs(Math.abs(nextRowCut - b) - lineCutDist) < lineSpacingHeight)
							doCut = true;
					}
					
					if (doCut) {
						float maxRowBrightness = 0f;
						for (int b = blockStart; b < blockEnd; b++) {
							if (maxRowBrightness < cutRowBrightnesses[b])
								maxRowBrightness = cutRowBrightnesses[b];
						}
						while (cutRowBrightnesses[blockStart] < maxRowBrightness)
							blockStart++;
						while (cutRowBrightnesses[blockEnd - 1] < maxRowBrightness)
							blockEnd--;
						
						for (int b = blockStart; b < blockEnd; b++) {
							for (int x = 0; x < width; x++)
								paint(x, b, Color.GREEN.getRGB(), image, false);
							rowCut[b] = true;
						}
					}
				}
				painted = 0;
			}
		}
		
		
		//	examine blocks of non-painted rows whose hight is a multiple of the line hight
		int unpainted = 0;
		for (int h = 0; h < height; h++) {
			if (rowPainted[h]) {
				if (unpainted != 0) {
					int blockStart = h-unpainted;
					int blockEnd = h;
					
					int lines = 0;
					
					int lastRowCut = blockStart;
					while (!rowCut[lastRowCut])
						lastRowCut--;
					int nextRowCut = blockEnd;
					while (!rowCut[nextRowCut])
						nextRowCut++;
					
					int cuttingContrastFactor = 3;
					float paintedRowBrightnessSum = 0f;
					painted = 0;
					for (int r = lastRowCut+1; r < nextRowCut; r++)
						if (rowPainted[r]) {
							paintedRowBrightnessSum += cutRowBrightnesses[r];
							painted++;
						}
					float paintedRowBrightness = ((painted == 0) ? 1f : (paintedRowBrightnessSum / painted));
					float cutBrightnessThreshold = (1f - (10 * (1f - paintedRowBrightness)));
					
					if ((lineCutDist + lineSpacingHeight) < (nextRowCut - lastRowCut)) {
						System.out.println("Found high uncut block between " + blockStart + " and " + blockEnd);
						System.out.println("  ==> cut brightness threshold is " + cutBrightnessThreshold);
						lines = 1;
					}
					
					for (int l = 2; ((l * (lineCutDist - lineSpacingHeight)) < (nextRowCut - lastRowCut)); l++)
						lines = l;
					
					
					if (lines == 1) {
						System.out.println("  ==> checking middle cut");
						for (int x = 0; x < width; x++)
							paint(x, ((lastRowCut + nextRowCut) / 2), Color.YELLOW.getRGB(), image, true);
						
						//	find most likely cut
						float minBrightness = 1f;
//						int minBrightnessRow = ((lastRowCut + nextRowCut) / 2);
						float maxBrightness = 0f;
						int maxBrightnessRow = ((lastRowCut + nextRowCut) / 2);
						for (int r = (((lastRowCut + nextRowCut) / 2) - lineSpacingHeight); r < (((lastRowCut + nextRowCut) / 2) + lineSpacingHeight); r++) {
							if (cutRowBrightnesses[r] < minBrightness) {
								minBrightness = cutRowBrightnesses[r];
//								minBrightnessRow = r;
							}
							if (cutRowBrightnesses[r] > maxBrightness) {
								maxBrightness = cutRowBrightnesses[r];
								maxBrightnessRow = r;
							}
						}
						System.out.println("  ==> brightness range is " + minBrightness + " - " + maxBrightness);
						if (!rowPainted[maxBrightnessRow] && ((maxBrightness >= cutBrightnessThreshold) || (((1f - maxBrightness) * cuttingContrastFactor) < (1f - minBrightness)))) {
							System.out.println("  ==> cut row brightness is above threshold, doing it");
							for (int x = 0; x < width; x++)
								paint(x, maxBrightnessRow, Color.YELLOW.getRGB(), image, false);
						}
					}
					
					else if (lines != 0) {
						System.out.println("  ==> likely multiline block between " + blockStart + " and " + blockEnd + ", " + lines + " lines");
						int lineHeight = ((nextRowCut - lastRowCut) / lines);
						System.out.println("  ==> check cutting in " + lineHeight + "-chunks");
						for (int l = 1; l < lines; l++) {
							for (int x = 0; x < width; x++)
								paint(x, (lastRowCut + (l * lineHeight)), Color.GREEN.getRGB(), image, true);
							
							//	find most likely cut
							float minBrightness = 1f;
//							int minBrightnessRow = (lastRowCut + (l * lineHeight));
							float maxBrightness = 0f;
							int maxBrightnessRow = (lastRowCut + (l * lineHeight));
							for (int r = (lastRowCut + (l * lineHeight) - Math.min((lines * lineSpacingHeight), (lineHeight / 3))); r < (lastRowCut + (l * lineHeight) + Math.min((lines * lineSpacingHeight), (lineHeight / 3))); r++) {
								if (cutRowBrightnesses[r] < minBrightness) {
									minBrightness = cutRowBrightnesses[r];
//									minBrightnessRow = r;
								}
								if (cutRowBrightnesses[r] > maxBrightness) {
									maxBrightness = cutRowBrightnesses[r];
									maxBrightnessRow = r;
								}
							}
							System.out.println("    ==> brightness range is " + minBrightness + " - " + maxBrightness);
							if ((maxBrightness >= cutBrightnessThreshold) || (((1f - maxBrightness) * cuttingContrastFactor) < (1f - minBrightness))) {
								System.out.println("    ==> cut row brightness is above threshold, doing it");
								for (int x = 0; x < width; x++)
									paint(x, maxBrightnessRow, Color.GREEN.getRGB(), image, false);
							}
						}
					}
					h = nextRowCut;
				}
				unpainted = 0;
			}
			else unpainted++;
		}
		
		JFrame frame = new JFrame();
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent we) {
				System.exit(0);
			}
			public void windowClosing(WindowEvent we) {
				System.exit(0);
			}
		});
		frame.setSize(500, 800);
		frame.setResizable(true);
		frame.setLocationRelativeTo(null);
		
		ImageViewerTest ivt = new ImageViewerTest(null);
		ivt.addImage(image);
		
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(ivt, BorderLayout.CENTER);
		
		frame.setVisible(true);
	}
	
	static float getTth(float[] data, int offset, boolean highest) {
		float[] sorted = new float[data.length];
		System.arraycopy(data, 0, sorted, 0, data.length);
		Arrays.sort(sorted);
		
		//	TODO: consider average of exteme elements
		return (highest ? sorted[sorted.length - 1 - offset] : sorted[offset]);
	}
	
	static float getPivot(float[] data, int offset) {
		int[] buckets = new int[200];
		for (int b = 0; b < buckets.length; b++)
			buckets[b] = 0;
		for (int d = 0; d < data.length; d++)
			buckets[Math.min((buckets.length - 1), ((int) (data[d] * buckets.length)))]++;
		for (int b = 0; b < buckets.length; b++)
			System.out.print(buckets[b] + " - ");
		for (int b = (buckets.length - 1); b > 0; b--) {
			if (buckets[b] < buckets[b-1]) {
				System.out.println("-> choose " + b);
				return (((float) b) / buckets.length);
			}
		}
		
		float[] sorted = new float[data.length];
		System.arraycopy(data, 0, sorted, 0, data.length);
		Arrays.sort(sorted);
		
		float maxDiff = 0f;
		float pivot = sorted[offset];
		for (int i = (offset + 1); i < (sorted.length - offset); i++) {
			float diff = sorted[i] - sorted[i-1];
			if (maxDiff < diff) {
				maxDiff = diff;
				pivot = sorted[i];
			}
		}
		
		return pivot;
	}
	
	static boolean paint(float minDiff, float maxDiff) {
		//return (maxDiff < minDiff);
		return (maxDiff*100 < minDiff);// TODO: optimize this
	}
	
	static void paint(int x, int y, int rgb, BufferedImage image, boolean mix) {
		
		if (mix) {
			int oRgb = image.getRGB(x, y);
			int ob = oRgb & 255;
			oRgb >>>= 8;
			int og = oRgb & 255;
			oRgb >>>= 8;
			int or = oRgb & 255;
			
			int b = rgb & 255;
			rgb >>>= 8;
			int g = rgb & 255;
			rgb >>>= 8;
			int r = rgb & 255;
			
			b = ((ob + b) / 2);
			g = ((og + g) / 2);
			r = ((or + r) / 2);
			
			rgb = r;
			rgb <<= 8;
			rgb = rgb | g;
			rgb <<= 8;
			rgb = rgb | b;
		}
		
		image.setRGB(x, y, rgb);
	}
	
	static int findBulkCenter(int[] data, int maxRadius) {
		int[] sums = new int[1 + (2 * maxRadius)];
		int[] maxSums = new int[sums.length];
		int[] maxSumCenters = new int[sums.length];
		
		for (int d = 0; d < data.length; d++) {
			for (int s = 0; s < sums.length; s++) {
				
				//	subtract data slipping out
				if (s < d) sums[s] -= data[d - s - 1];
				
				//	add new data
				sums[s] += data[d];
				
				//	check if new maximum
				if (sums[s] > maxSums[s]) {
					maxSums[s] = sums[s];
					
					//	if so, remember center
					maxSumCenters[s] = (d - (s / 2));
				}
			}
		}
		
		//	get most frequent center
		Arrays.sort(maxSumCenters);
		int maxCenterSum = 0;
		for (int s = 0; s < maxSumCenters.length; s++)
			maxCenterSum += maxSumCenters[s];
		
		//	return average if contained in array (might not be if there's two or more dense regions in the data)
		int center = (maxCenterSum / sums.length);
		for (int s = 0; s < maxSumCenters.length; s++)
			if (maxSumCenters[s] == center)
				return center;
		
		//	return median otherwise
		return maxSumCenters[maxRadius];
	}
	
	static float cutBrightnessThreshold = 0.96875f;
	static BufferedImage cutImage(BufferedImage image) {
		System.out.println("   - truncating image ...");
		
		int width = image.getWidth();
		int height = image.getHeight();
		System.out.println("     - size is " + width + " x " + height);
		
		boolean canCut;
		
		int leftCut = 0;
		canCut = true;
		while ((leftCut < width) && canCut) {
			float[] hsb = null;
			for (int y = 0; y < height; y++) {
				hsb = new Color(image.getRGB(leftCut, y)).getColorComponents(hsb);
				if (hsb[2] < cutBrightnessThreshold) canCut = false;
			}
			if (canCut) leftCut++;
		}
		System.out.println("     - left cut: " + leftCut);
		
		int rightCut = 0;
		canCut = true;
		while ((rightCut < width) && canCut) {
			float[] hsb = null;
			for (int y = 0; y < height; y++) {
				hsb = new Color(image.getRGB((width - rightCut - 1), y)).getColorComponents(hsb);
				if (hsb[2] < cutBrightnessThreshold) canCut = false;
			}
			if (canCut) rightCut++;
		}
		System.out.println("     - right cut: " + rightCut);
		
		int topCut = 0;
		canCut = true;
		while ((topCut < height) && canCut) {
			float[] hsb = null;
			for (int x = leftCut; x < (width - rightCut); x++) {
				hsb = new Color(image.getRGB(x, topCut)).getColorComponents(hsb);
				if (hsb[2] < cutBrightnessThreshold) canCut = false;
			}
			if (canCut) topCut++;
		}
		System.out.println("     - top cut: " + topCut);
		
		int bottomCut = 0;
		canCut = true;
		while ((bottomCut < height) && canCut) {
			float[] hsb = null;
			for (int x = leftCut; x < (width - rightCut); x++) {
				hsb = new Color(image.getRGB(x, (height - bottomCut - 1))).getColorComponents(hsb);
				if (hsb[2] < cutBrightnessThreshold) canCut = false;
			}
			if (canCut) bottomCut++;
		}
		System.out.println("     - bottom cut: " + bottomCut);
		
		if ((topCut + bottomCut + leftCut + rightCut) == 0) return image;
		else return image.getSubimage(leftCut, topCut, (width - leftCut - rightCut), (height - topCut - bottomCut));
	}
	
	static BufferedImage cloneImage(BufferedImage source) {
		int width = source.getWidth();
		int height = source.getHeight();
		BufferedImage clone = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (int w = 0; w < width; w++)
			for (int h = 0; h < height; h++)
				clone.setRGB(w, h, source.getRGB(w, h));
		
		return clone;
	}
	
	static class ImageViewerTest extends JPanel {
		
		private boolean fitWidth = false;
		private boolean fitHeight = false;
		
		private ImageTray imageTray;
		private JScrollPane imageBox;
		
		private static final String[] zoomFactors = {"10", "25", "33", "50", "75", "100", "125", "150", "200", "Fit Size", "Fit Width", "Fit Height"};
		private JComboBox zoomer = new JComboBox(zoomFactors);
		
		private JButton setImageButton = new JButton("Open Image");
		private JFileChooser setImageSelector = new JFileChooser();
		
		public ImageViewerTest(File imageFile) {
			super(new BorderLayout(), true);
			
			this.imageTray = new ImageTray();
			
			this.imageBox = new JScrollPane(this.imageTray);
			this.imageBox.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent ce) {
					Dimension size = imageBox.getSize();
					
					if ((2 * size.width) < size.height)
						imageTray.setUseVerticalLayout(true);
					else if ((2 * size.height) < size.width)
						imageTray.setUseVerticalLayout(false);
					
					if (fitWidth && fitHeight) imageTray.fitSize();
					else if (fitWidth) imageTray.fitWidth();
					else if (fitHeight) imageTray.fitHeight();
				}
			});
			
			this.setImageSelector.setFileSelectionMode(JFileChooser.FILES_ONLY);
			this.setImageSelector.setMultiSelectionEnabled(false);
			
			this.setImageButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.setImageButton.setPreferredSize(new Dimension(100, 21));
			this.setImageButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					addImage();
				}
			});
			
			this.zoomer.setBorder(BorderFactory.createLoweredBevelBorder());
			this.zoomer.setPreferredSize(new Dimension(100, 21));
			this.zoomer.setMaximumRowCount(zoomFactors.length);
			this.zoomer.setSelectedItem("100");
			this.zoomer.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					String selected = zoomer.getSelectedItem().toString();
					if ("Fit Size".equals(selected)) {
						fitWidth = true;
						fitHeight = true;
						imageTray.fitSize();
					}
					else if ("Fit Width".equals(selected)) {
						fitWidth = true;
						fitHeight = false;
						imageTray.fitWidth();
					}
					else if ("Fit Height".equals(selected)) {
						fitWidth = false;
						fitHeight = true;
						imageTray.fitHeight();
					}
					else {
						fitWidth = false;
						fitHeight = false;
						float factor = Integer.parseInt(selected);
						imageTray.zoom(factor / 100);
					}
				}
			});
			JPanel functionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			functionPanel.add(this.setImageButton);
			functionPanel.add(this.zoomer);
			
			this.add(functionPanel, BorderLayout.NORTH);
			this.add(this.imageBox, BorderLayout.CENTER);
		}
		
		private void addImage() {
			if (this.setImageSelector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				File newFile = this.setImageSelector.getSelectedFile();
				
				if (newFile == null) return;
				
				Image image = null;
				try {
					image = ImageIO.read(newFile);
					
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				
				if (image != null)
					this.addImage(image);
			}
		}
		
		public void addImage(Image image) {
			this.imageTray.addImage(image);
			String selected = this.zoomer.getSelectedItem().toString();
			if ("Fit Size".equals(selected)) {
				fitWidth = true;
				fitHeight = true;
				imageTray.fitSize();
			}
			else if ("Fit Width".equals(selected)) {
				fitWidth = true;
				fitHeight = false;
				imageTray.fitWidth();
			}
			else if ("Fit Height".equals(selected)) {
				fitWidth = false;
				fitHeight = true;
				imageTray.fitHeight();
			}
			else {
				fitWidth = false;
				fitHeight = false;
				float factor = Integer.parseInt(selected);
				imageTray.zoom(factor / 100);
			}
		}
		
		private class ImageTray extends JPanel {
			
			private int maxImages = 5;
			private LinkedList images = new LinkedList();
			
			private boolean isVerticalLayout = false;
			private GridBagConstraints gbc = new GridBagConstraints();
			
			private ImageTray() {
				super(new GridBagLayout(), true);
				
				this.gbc.insets.top = 3;
				this.gbc.insets.bottom = 3;
				this.gbc.insets.left = 3;
				this.gbc.insets.right = 3;
				
				this.gbc.weighty = 0;
				this.gbc.weightx = 0;
				
				this.gbc.gridheight = 1;
				this.gbc.gridwidth = 1;
				
				this.gbc.fill = GridBagConstraints.NONE;
				
				this.gbc.gridy = 0;
				this.gbc.gridx = 0;
			}
			
			private void addImage(Image image) {
				while (this.images.size() >= this.maxImages)
					this.images.removeFirst();
				
				ImagePanel ip = new ImagePanel(image, true);
				this.images.addLast(ip);
				
				this.layoutImages();
			}
			
			private void setUseVerticalLayout(boolean useVerticalLayout) {
				if (this.isVerticalLayout != useVerticalLayout) {
					this.isVerticalLayout = useVerticalLayout;
					this.layoutImages();
				}
			}
			
			private void layoutImages() {
				this.removeAll();
				
				GridBagConstraints gbc = ((GridBagConstraints) this.gbc.clone());
				
				for (Iterator ii = this.images.iterator(); ii.hasNext();) {
					ImagePanel image = ((ImagePanel) ii.next());
					
					this.add(image, gbc.clone());
					
					if (this.isVerticalLayout) gbc.gridy++;
					else gbc.gridx++;
				}
				
				this.revalidate();
			}
			
			private void zoom(float factor) {
				for (Iterator ii = this.images.iterator(); ii.hasNext();)
					((ImagePanel) ii.next()).zoom(factor);
				
				this.revalidate();
			}
			
			private void fitSize() {
				Dimension size = imageBox.getViewport().getExtentSize();
				float widthFactor = ((float) (size.width - 6)) / this.getImageWidth();
				float heightFactor = ((float) (size.height - 6)) / this.getImageHeight();
				
				this.zoom(Math.min(widthFactor, heightFactor));
			}
			private void fitWidth() {
				Dimension size = imageBox.getViewport().getExtentSize();
				JScrollBar vScroll = imageBox.getVerticalScrollBar();
				
				this.zoom(((float) (size.width - 6 - (vScroll.isShowing() ? vScroll.getWidth() : 0))) / this.getImageWidth());
			}
			private void fitHeight() {
				Dimension size = imageBox.getViewport().getExtentSize();
				JScrollBar hScroll = imageBox.getHorizontalScrollBar();
				
				this.zoom(((float) (size.height - 6 - (hScroll.isShowing() ? hScroll.getHeight() : 0))) / this.getImageHeight());
			}
			
			private int getImageWidth() {
				int width = 0;
				
				for (Iterator ii = this.images.iterator(); ii.hasNext();) {
					ImagePanel image = ((ImagePanel) ii.next());
					
					if (this.isVerticalLayout) width = Math.max(width, image.imageWidth);
					else width = (width + 6 + image.imageWidth);
				}
				
				return width;
			}
			private int getImageHeight() {
				int height = 0;
				
				for (Iterator ii = this.images.iterator(); ii.hasNext();) {
					ImagePanel image = ((ImagePanel) ii.next());
					
					if (this.isVerticalLayout) height = (height + 6 + image.imageHeight);
					else height = Math.max(height, image.imageHeight);
				}
				
				return height;
			}
		}
		
		private static class ImagePanel extends JPanel {
			private Image image = null;
			private int imageWidth;
			private int imageHeight;
			
			private ImagePanel(Image image, boolean isInTray) {
				super(true);
				this.setBackground(Color.WHITE);
				
				this.image = image;
				
				this.imageHeight = this.image.getHeight(null);
				this.imageWidth = this.image.getWidth(null);
				Dimension dim = new Dimension(this.imageWidth, this.imageHeight);
				
				this.setMinimumSize(dim);
				this.setPreferredSize(dim);
				this.setMaximumSize(dim);
			}
			
			private void zoom(float factor) {
//				System.out.println("Zoom factor set to " + factor);
				Dimension dim = new Dimension(((int) (this.imageWidth * factor)), ((int) (this.imageHeight * factor)));
				
				this.setMinimumSize(dim);
				this.setPreferredSize(dim);
				this.setMaximumSize(dim);
			}
			
			public void paintComponent(Graphics graphics) {
				super.paintComponent(graphics);
				if (this.image != null) graphics.drawImage(this.image, 0, 0, this.getWidth(), this.getHeight(), this);
			}
		}
	}
}
