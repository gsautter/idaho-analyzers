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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

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
public class LineExtractorTest2 {
	
	static class Rectangle {
		final BufferedImage image;
		final float[][] brightness;
//		final boolean[][] isWhite;
		int topRow; // inclusive
		int bottomRow; // exclusive
		int leftCol; // inclusive
		int rightCol; // exclusive
		
//		Rectangle(BufferedImage image) {
//			this.image = image;
//			this.leftCol = 0;
//			this.rightCol = this.image.getWidth();
//			this.topRow = 0;
//			this.bottomRow = this.image.getHeight();
//		}
		Rectangle(BufferedImage image) {
			this.image = image;
			
			this.leftCol = 0;
			this.rightCol = this.image.getWidth();
			this.topRow = 0;
			this.bottomRow = this.image.getHeight();
			
			this.brightness = new float[this.image.getWidth()][this.image.getHeight()];
//			this.isWhite = new boolean[this.image.getWidth()][this.image.getHeight()];
			
			float[] hsb = null;
			int rgb;
			for (int c = this.leftCol; c < this.rightCol; c++) {
				for (int r = this.topRow; r < this.bottomRow; r++) {
					rgb = this.image.getRGB(c, r);
					hsb = new Color(rgb).getColorComponents(hsb);
					this.brightness[c][r] = hsb[2];
//					this.isWhite[c][r] = (hsb[2] == 1f);
				}
			}
		}
		
//		Rectangle(BufferedImage image, float[][] brightnesses, boolean[][] isWhite) {
		Rectangle(BufferedImage image, float[][] brightnesses) {
			this.image = image;
			
			this.leftCol = 0;
			this.rightCol = this.image.getWidth();
			this.topRow = 0;
			this.bottomRow = this.image.getHeight();
			
			this.brightness = brightnesses;
//			this.isWhite = isWhite;
		}
	}
	
	static Rectangle narrowCol(Rectangle rect, boolean useMinMax, int offset) {
//		float[] colBrightnesses = new float[rect.rightCol - rect.leftCol];
//		for (int c = rect.leftCol; c < rect.rightCol; c++) {
//			float brightnessSum = 0f; 
//			for (int r = rect.topRow; r < rect.bottomRow; r++)
//				brightnessSum += rect.brightness[c][r];
//			colBrightnesses[c - rect.leftCol] = (brightnessSum / (rect.bottomRow - rect.topRow));
//		}
		float[] colBrightnesses = new float[rect.rightCol - rect.leftCol];
		float[] colMinBrightnesses = new float[rect.rightCol - rect.leftCol];
		for (int c = rect.leftCol; c < rect.rightCol; c++) {
			float brightnessSum = 0;
			float colMinBrightness = 1;
			for (int r = rect.topRow; r < rect.bottomRow; r++) {
				brightnessSum += rect.brightness[c][r];
				colMinBrightness = Math.min(colMinBrightness, rect.brightness[c][r]);
			}
			colBrightnesses[c - rect.leftCol] = (brightnessSum / (rect.bottomRow - rect.topRow));
			colMinBrightnesses[c - rect.leftCol] = colMinBrightness;
		}
		
//		Rectangle res = new Rectangle(rect.image, rect.brightness, rect.isWhite);
		Rectangle res = new Rectangle(rect.image, rect.brightness);
		res.topRow = rect.topRow;
		res.bottomRow = rect.bottomRow;
		
		if (useMinMax) {
			
			float minColBrightness = getTth(colBrightnesses, offset, false);
			float maxColBrightness = getTth(colBrightnesses, offset, true);
//			System.out.println("ColBrightness: " + minColBrightness + " - " + maxColBrightness);
			
			//	find to-paint cols
			int minCol = -1;
			int maxCol = rect.rightCol;
			for (int c = rect.leftCol; c < rect.rightCol; c++) {
				float colBrightness = colBrightnesses[c - rect.leftCol];
				float minDiff = Math.abs(colBrightness - minColBrightness);
				float maxDiff = Math.abs(colBrightness - maxColBrightness);
				
				if (!isPainteable(minDiff, maxDiff)) {
					if (minCol == -1)
						minCol = c;
					maxCol = c;
				}
			}
			
			res.leftCol = minCol;
			res.rightCol = maxCol+1;
		}
		else {
			
			float colBrightnessPivot = 1f;//getPivot(colBrightnesses, offset);
//			float colBrightnessPivot = getPivot(colBrightnesses, ((rect.rightCol - rect.leftCol) / 100));
//			System.out.println("ColBrightness: " + colBrightnessPivot);
			
			//	find to-paint cols
			int minCol = -1;
			int maxCol = rect.rightCol;
			for (int c = rect.leftCol; c < rect.rightCol; c++) {
				if (((colBrightnessPivot == 1f) ? (colBrightnesses[c - rect.leftCol] < colBrightnessPivot) : (colBrightnesses[c - rect.leftCol] <= colBrightnessPivot)) && (colMinBrightnesses[c - rect.leftCol] < 0.9)) {
					if (minCol == -1)
						minCol = c;
					maxCol = c;
				}
			}
			
			if (minCol == -1) {
				res.leftCol = 0;
				res.rightCol = 0;
			}
			else {
				res.leftCol = minCol;
				res.rightCol = maxCol+1;
			}
		}
		
		return res;
	}
	
	static Rectangle narrowRow(Rectangle rect, boolean useMinMax, int offset) {
//		float[] rowBrightnesses = new float[rect.bottomRow - rect.topRow];
//		for (int r = rect.topRow; r < rect.bottomRow; r++) {
//			float brightnessSum = 0f; 
//			for (int c = rect.leftCol; c < rect.rightCol; c++)
//				brightnessSum += rect.brightness[c][r];
//			rowBrightnesses[r - rect.topRow] = (brightnessSum / (rect.rightCol - rect.leftCol));
//		}
		float[] rowBrightnesses = new float[rect.bottomRow - rect.topRow];
		float[] rowMinBrightnesses = new float[rect.bottomRow - rect.topRow];
		for (int r = rect.topRow; r < rect.bottomRow; r++) {
			float brightnessSum = 0; 
			float minBrightness = 1;
			for (int c = rect.leftCol; c < rect.rightCol; c++)  {
				brightnessSum += rect.brightness[c][r];
				minBrightness = Math.min(minBrightness, rect.brightness[c][r]);
			}
			rowBrightnesses[r - rect.topRow] = (brightnessSum / (rect.rightCol - rect.leftCol));
			rowMinBrightnesses[r - rect.topRow] = minBrightness;
		}
		
//		Rectangle res = new Rectangle(rect.image, rect.brightness, rect.isWhite);
		Rectangle res = new Rectangle(rect.image, rect.brightness);
		res.leftCol = rect.leftCol;
		res.rightCol = rect.rightCol;
		
		if (useMinMax) {
			
			float minRowBrightness = getTth(rowBrightnesses, offset, false);
			float maxRowBrightness = getTth(rowBrightnesses, offset, true);
//			System.out.println("RowBrightness: " + minRowBrightness + " - " + maxRowBrightness);
			
			//	find to-paint rows
			int minRow = -1;
			int maxRow = rect.bottomRow;
			for (int r = rect.topRow; r < rect.bottomRow; r++) {
				float rowBrightness = rowBrightnesses[r - rect.topRow];
				float minDiff = Math.abs(rowBrightness - minRowBrightness);
				float maxDiff = Math.abs(rowBrightness - maxRowBrightness);
				
				if (!isPainteable(minDiff, maxDiff)) {
					if (minRow == -1)
						minRow = r;
					maxRow = r;
				}
			}
			
			res.topRow = minRow;
			res.bottomRow = maxRow+1;
		}
		else {
			
			float rowBrightnessPivot = 1f;//getPivot(rowBrightnesses, offset);
//			float rowBrightnessPivot = getPivot(rowBrightnesses, ((rect.bottomRow - rect.topRow) / 100));
//			System.out.println("RowBrightness: " + rowBrightnessPivot);
			
			//	find to-paint cols
			int minRow = -1;
			int maxRow = rect.bottomRow;
			for (int r = rect.topRow; r < rect.bottomRow; r++) {
				if (((rowBrightnessPivot == 1f) ? (rowBrightnesses[r - rect.topRow] < rowBrightnessPivot) : (rowBrightnesses[r - rect.topRow] <= rowBrightnessPivot)) && (rowMinBrightnesses[r - rect.topRow] < 0.9)) {
					if (minRow == -1)
						minRow = r;
					maxRow = r;
				}
			}
			
			if (minRow == -1) {
				res.topRow = 0;
				res.bottomRow = 0;
			}
			else {
				res.topRow = minRow;
				res.bottomRow = maxRow+1;
			}
		}
		
		return res;
	}
	
	static Rectangle[] splitIntoCols(Rectangle rect, boolean useMinMax, int offset) {
		float[] colBrightnesses = new float[rect.rightCol - rect.leftCol];
		for (int c = rect.leftCol; c < rect.rightCol; c++) {
			float brightnessSum = 0f; 
			for (int r = rect.topRow; r < rect.bottomRow; r++)
				brightnessSum += rect.brightness[c][r];
			colBrightnesses[c - rect.leftCol] = (brightnessSum / (rect.bottomRow - rect.topRow));
		}
		
		ArrayList rects = new ArrayList();
		boolean[] paintCols = new boolean[rect.rightCol - rect.leftCol];
		
		if (useMinMax) {
			
			float minColBrightness = getTth(colBrightnesses, offset, false);
			float maxColBrightness = getTth(colBrightnesses, offset, true);
//			System.out.println("ColBrightness: " + minColBrightness + " - " + maxColBrightness);
			
			//	find to-paint cols
			for (int c = rect.leftCol; c < rect.rightCol; c++) {
				float colBrightness = colBrightnesses[c - rect.leftCol];
				float minDiff = Math.abs(colBrightness - minColBrightness);
				float maxDiff = Math.abs(colBrightness - maxColBrightness);
				
				paintCols[c - rect.leftCol] = isPainteable(minDiff, maxDiff);
			}
		}
		
		else {
			
			float colBrightnessPivot = 1f;//getPivot(colBrightnesses, offset);
//			float colBrightnessPivot = getPivot(colBrightnesses, ((rect.rightCol - rect.leftCol) / 100));
//			System.out.println("ColBrightness: " + colBrightnessPivot);
			
			//	find to-paint cols
			for (int c = rect.leftCol; c < rect.rightCol; c++)
				paintCols[c - rect.leftCol] = (colBrightnesses[c - rect.leftCol] >= colBrightnessPivot);
		}
		
		int unpainted = 0;
		for (int c = rect.leftCol; c <= rect.rightCol; c++) {
			if ((c == rect.rightCol) || paintCols[c - rect.leftCol]) {
				
				if (unpainted != 0) {
					
//					Rectangle res = new Rectangle(rect.image, rect.brightness, rect.isWhite);
					Rectangle res = new Rectangle(rect.image, rect.brightness);
					res.topRow = rect.topRow;
					res.bottomRow = rect.bottomRow;
					
					res.leftCol = (c - unpainted);
					res.rightCol = c;
					rects.add(res);
				}
				
				unpainted = 0;
			}
			else unpainted++;
		}
		
		return ((Rectangle[]) rects.toArray(new Rectangle[rects.size()]));
	}
	
	static Rectangle[] splitIntoRows(Rectangle rect, boolean useMinMax, int offset) {
		float[] rowBrightnesses = new float[rect.bottomRow - rect.topRow];
		for (int r = rect.topRow; r < rect.bottomRow; r++) {
			float brightnessSum = 0f; 
			for (int c = rect.leftCol; c < rect.rightCol; c++)
				brightnessSum += rect.brightness[c][r];
			rowBrightnesses[r - rect.topRow] = (brightnessSum / (rect.rightCol - rect.leftCol));
		}
		
		ArrayList rects = new ArrayList();
		boolean[] paintRows = new boolean[rect.bottomRow - rect.topRow];
		
		if (useMinMax) {
			
			float minRowBrightness = getTth(rowBrightnesses, offset, false);
			float maxRowBrightness = getTth(rowBrightnesses, offset, true);
//			System.out.println("RowBrightness: " + minRowBrightness + " - " + maxRowBrightness);
			
			//	find to-paint rows
			for (int r = rect.topRow; r < rect.bottomRow; r++) {
				float rowBrightness = rowBrightnesses[r - rect.topRow];
				float minDiff = Math.abs(rowBrightness - minRowBrightness);
				float maxDiff = Math.abs(rowBrightness - maxRowBrightness);
				
				paintRows[r - rect.topRow] = isPainteable(minDiff, maxDiff);
			}
		}
		
		else {
			
			float rowBrightnessPivot = 1f;//getPivot(rowBrightnesses, offset);
//			float rowBrightnessPivot = getPivot(rowBrightnesses, ((rect.bottomRow - rect.topRow) / 100));
//			System.out.println("RowBrightness: " + rowBrightnessPivot);
			
			//	find to-paint cols
			for (int r = rect.topRow; r < rect.bottomRow; r++)
				paintRows[r - rect.topRow] = (rowBrightnesses[r - rect.topRow] >= rowBrightnessPivot);
		}
		
		int unpainted = 0;
		for (int c = rect.topRow; c <= rect.bottomRow; c++) {
			if ((c == rect.bottomRow) || paintRows[c - rect.topRow]) {
				
				if (unpainted != 0) {
					
//					Rectangle res = new Rectangle(rect.image, rect.brightness, rect.isWhite);
					Rectangle res = new Rectangle(rect.image, rect.brightness);
					res.leftCol = rect.leftCol;
					res.rightCol = rect.rightCol;
					
					res.topRow = (c - unpainted);
					res.bottomRow = c;
					rects.add(res);
				}
				
				unpainted = 0;
			}
			else unpainted++;
		}
		
		return ((Rectangle[]) rects.toArray(new Rectangle[rects.size()]));
	}
	
	static Rectangle[] smoothenRowSplit(Rectangle[] rowRects, float minBrightness) {
		int maxRowHeight = 0;
		for (int rr = 0; rr < rowRects.length; rr++)
			if (maxRowHeight < (rowRects[rr].bottomRow - rowRects[rr].topRow))
				maxRowHeight = (rowRects[rr].bottomRow - rowRects[rr].topRow);
		
		int[] rowHeightBuckets = new int[maxRowHeight + 1];
		for (int r = 0; r < rowRects.length; r++)
			rowHeightBuckets[rowRects[r].bottomRow - rowRects[r].topRow]++;
		
		int averageRowHeight = findBulkCenter(rowHeightBuckets, 5);
		averageRowHeight = Math.max(1, averageRowHeight);
		System.out.println("Average Row Height (==> Line Height): " + averageRowHeight);
		
		ArrayList rowRectList = new ArrayList();
		for (int rr = 0; rr < rowRects.length; rr++) {
			int lines = 1;
			while ((lines * averageRowHeight) < (rowRects[rr].bottomRow - rowRects[rr].topRow))
				lines++;
			
			if (lines == 1) {
				System.out.println("Keeping single line [" + rowRects[rr].leftCol + "-" + rowRects[rr].rightCol + "] x [" + rowRects[rr].topRow + "-" + rowRects[rr].bottomRow + "]");
				rowRectList.add(rowRects[rr]);
			}
			
			else {
				System.out.println("Trying to split [" + rowRects[rr].leftCol + "-" + rowRects[rr].rightCol + "] x [" + rowRects[rr].topRow + "-" + rowRects[rr].bottomRow + "] ...");
				
				int[] lefts = new int[rowRects[rr].bottomRow - rowRects[rr].topRow];
				int[] rights = new int[rowRects[rr].bottomRow - rowRects[rr].topRow];
				int maxRemain = ((rowRects[rr].rightCol - rowRects[rr].leftCol) / 2);
				int maxFuzzyness = 7;
				
				for (int r = rowRects[rr].topRow; r < rowRects[rr].bottomRow; r++) {
					lefts[r - rowRects[rr].topRow] = rowRects[rr].rightCol;
					for (int c = rowRects[rr].leftCol; c < rowRects[rr].rightCol; c++) {
						if (rowRects[rr].brightness[c][r] < minBrightness) {
							lefts[r - rowRects[rr].topRow] = c;
							c = rowRects[rr].rightCol;
						}
					}
					
					rights[r - rowRects[rr].topRow] = rowRects[rr].leftCol;
					for (int c = rowRects[rr].rightCol; c > rowRects[rr].leftCol; c--) {
						if (rowRects[rr].brightness[c-1][r] < minBrightness) {
							rights[r - rowRects[rr].topRow] = c;
							c = rowRects[rr].leftCol;
						}
					}
				}
				
				int minLineLoss = getRowSplitLoss(rowRects[rr].leftCol, lefts, rowRects[rr].rightCol, rights, (rowRects[rr].bottomRow - rowRects[rr].topRow), lines-1, maxFuzzyness);
				System.out.println(" - loss with " + (lines-1) + " lines is " + minLineLoss);
				int maxLineLoss = getRowSplitLoss(rowRects[rr].leftCol, lefts, rowRects[rr].rightCol, rights, (rowRects[rr].bottomRow - rowRects[rr].topRow), lines, maxFuzzyness);
				System.out.println(" - loss with " + lines + " lines is " + maxLineLoss);
				
				if (minLineLoss < maxLineLoss)
					lines = lines-1;
				
				if (Math.min(minLineLoss, maxLineLoss) < maxRemain) {
					System.out.println(" - trying " + lines + " lines ...");
					int[] splitRows = new int[lines + 1];
					splitRows[0] = rowRects[rr].topRow;
					splitRows[lines] = rowRects[rr].bottomRow;
					
					int fuzzyness = Math.min(maxFuzzyness, ((rowRects[rr].bottomRow - rowRects[rr].topRow) / (lines * 2)));
					
					for (int s = 1; s < lines; s++) {
						int splitCenter = ((s * (rowRects[rr].bottomRow - rowRects[rr].topRow)) / lines);
						System.out.println("   - doing split " + s + " around " + splitCenter);
						
						int maxLeft = rowRects[rr].leftCol;
						int maxLeftRow = splitCenter;
						int minRight = rowRects[rr].rightCol;
						int minRightRow = splitCenter;
						int minRemain = (rowRects[rr].rightCol - rowRects[rr].leftCol);
						int minRemainRow = splitCenter;
						
						for (int f = 0; f <= fuzzyness; f++) {
							if (lefts[splitCenter + f] > maxLeft) {
								maxLeft = lefts[splitCenter + f];
								maxLeftRow = (splitCenter + f);
							}
							if (lefts[splitCenter - f] > maxLeft) {
								maxLeft = lefts[splitCenter - f];
								maxLeftRow = (splitCenter - f);
							}
							
							if (rights[splitCenter + f] < minRight) {
								minRight = rights[splitCenter + f];
								minRightRow = (splitCenter + f);
							}
							if (rights[splitCenter - f] < minRight) {
								minRight = rights[splitCenter - f];
								minRightRow = (splitCenter - f);
							}
							
							if ((rights[splitCenter + f] - lefts[splitCenter + f]) < minRemain) {
								minRemain = (rights[splitCenter + f] - lefts[splitCenter + f]);
								minRemainRow = (splitCenter + f);
							}
							if ((rights[splitCenter - f] - lefts[splitCenter - f]) < minRemain) {
								minRemain = (rights[splitCenter - f] - lefts[splitCenter - f]);
								minRemainRow = (splitCenter - f);
							}
						}
						
						System.out.println("     - max left is " + maxLeft + " in " + maxLeftRow);
						System.out.println("     - min right is " + minRight + " in " + minRightRow);
						System.out.println("     - min remain is " + minRemain + " in " + minRemainRow);
						
						if (maxLeftRow == minRightRow)
							splitRows[s] = (minRemainRow + rowRects[rr].topRow);
						else splitRows[s] = (((maxLeftRow + minRightRow) / 2) + rowRects[rr].topRow);
					}
					
					for (int s = 0; s < lines; s++) {
//						Rectangle splitRect = new Rectangle(rowRects[rr].image, rowRects[rr].brightness, rowRects[rr].isWhite);
						Rectangle splitRect = new Rectangle(rowRects[rr].image, rowRects[rr].brightness);
						splitRect.leftCol = rowRects[rr].leftCol;
						splitRect.rightCol = rowRects[rr].rightCol;
						splitRect.topRow = splitRows[s];
						splitRect.bottomRow = splitRows[s+1];
						rowRectList.add(splitRect);
					}
				}
				
				else rowRectList.add(rowRects[rr]);
			}
		}
		
		return ((Rectangle[]) rowRectList.toArray(new Rectangle[rowRectList.size()]));
	}
	
	static int getRowSplitLoss(int left, int[] lefts, int right, int[] rights, int splitRowHeight, int lines, int fuzzyness) {
		fuzzyness = Math.min(fuzzyness, (splitRowHeight / (lines * 2)));
		int remainSum = 0;
		for (int s = 1; s < lines; s++) {
			int splitCenter = ((s * splitRowHeight) / lines);
			
			int maxLeft = left;
			int minRight = right;
			int minRemain = (right - left);
			
			for (int f = 0; f <= fuzzyness; f++) {
				if (lefts[splitCenter + f] > maxLeft)
					maxLeft = lefts[splitCenter + f];
				if (lefts[splitCenter - f] > maxLeft)
					maxLeft = lefts[splitCenter - f];
				
				if (rights[splitCenter + f] < minRight)
					minRight = rights[splitCenter + f];
				if (rights[splitCenter - f] < minRight)
					minRight = rights[splitCenter - f];
				
				if ((rights[splitCenter + f] - lefts[splitCenter + f]) < minRemain)
					minRemain = (rights[splitCenter + f] - lefts[splitCenter + f]);
				if ((rights[splitCenter - f] - lefts[splitCenter - f]) < minRemain)
					minRemain = (rights[splitCenter - f] - lefts[splitCenter - f]);
			}
			
			remainSum += Math.min(minRemain, (minRight - maxLeft));
		}
		
		return ((lines == 1) ? (right - left) : (remainSum / (lines - 1)));
	}
	
	static void paintRect(Rectangle rect, Color color, boolean transparent) {
		int rgb = color.getRGB();
		for (int x = rect.leftCol; x < rect.rightCol; x++) {
			paint(x, rect.topRow, rgb, rect.image, transparent);
			paint(x, (rect.bottomRow-1), rgb, rect.image, transparent);
		}
		for (int y = rect.topRow; y < rect.bottomRow; y++) {
			paint(rect.leftCol, y, rgb, rect.image, transparent);
			paint((rect.rightCol-1), y, rgb, rect.image, transparent);
		}
	}
	
	static void whitenWhite(Rectangle rect) {
		double brightnessSum = 0f;
		for (int r = rect.topRow; r < rect.bottomRow; r++) {
			for (int c = rect.leftCol; c < rect.rightCol; c++)
				brightnessSum += rect.brightness[c][r];
		}
		
		double brightness = (brightnessSum / ((rect.bottomRow - rect.topRow) * (rect.rightCol - rect.leftCol)));
		System.out.println("Average Brightness: " + brightness);
		
		int whitened = 0;
		for (int r = rect.topRow; r < rect.bottomRow; r++) {
			for (int c = rect.leftCol; c < rect.rightCol; c++) {
				if (rect.brightness[c][r] > brightness) {
					rect.brightness[c][r] = 1f;
					paint(c, r, Color.WHITE.getRGB(), rect.image, false);
					whitened++;
				}
			}
		}
		System.out.println("Whitened " + whitened + " of " + ((rect.bottomRow - rect.topRow) * (rect.rightCol - rect.leftCol)) + " pixels");
		brightnessSum = 0f;
		for (int r = rect.topRow; r < rect.bottomRow; r++) {
			for (int c = rect.leftCol; c < rect.rightCol; c++)
				brightnessSum += rect.brightness[c][r];
		}
		brightness = (brightnessSum / ((rect.bottomRow - rect.topRow) * (rect.rightCol - rect.leftCol)));
		System.out.println("New Average Brightness: " + brightness);
	}
	
	static void paintIndents(Rectangle rect, Color leftColor, Color rightColor, float minBrightness, int minIndent) {
		for (int r = rect.topRow; r < rect.bottomRow; r++) {
			int left = rect.rightCol;
			int right = rect.leftCol;
			for (int c = rect.leftCol; c < rect.rightCol; c++) {
				if (rect.brightness[c][r] < minBrightness) {
					left = c;
					c = rect.rightCol;
				}
			}
			for (int c = rect.rightCol; c > rect.leftCol; c--) {
				if (rect.brightness[c][r] < minBrightness) {
					right = c;
					c = rect.leftCol;
				}
			}
			if ((left - rect.leftCol) >= minIndent)
				for (int c = rect.leftCol; c < left; c++)
					paint(c, r, leftColor.getRGB(), rect.image, true);
				
			if ((rect.rightCol - right) >= minIndent)
				for (int c = right; c < rect.rightCol; c++)
					paint(c, r, rightColor.getRGB(), rect.image, true);
		}
	}
	
	static Rectangle[] featherDust(Rectangle rect, boolean rowsFirst) {
		ArrayList eliminatable = new ArrayList();
		
		if (rowsFirst) {
			Rectangle[] rows = splitIntoRows(rect, false, 0);
			Rectangle[][] rowCols = new Rectangle[rows.length][];
			Rectangle[][][] rowColRows = new Rectangle[rows.length][][];
			Rectangle[][][][] rowColRowCols = new Rectangle[rows.length][][][];
			
			for (int r = 0; r < rows.length; r++) {
				rows[r] = narrowCol(rows[r], false, 0);
				rows[r] = narrowRow(rows[r], false, 0);
				
				rowCols[r] = splitIntoCols(rows[r], false, 0);
				
				rowColRows[r] = new Rectangle[rowCols[r].length][];
				rowColRowCols[r] = new Rectangle[rowCols[r].length][][];
				
				for (int c = 0; c < rowCols[r].length; c++) {
					rowCols[r][c] = narrowRow(rowCols[r][c], false, 0);
					rowCols[r][c] = narrowCol(rowCols[r][c], false, 0);
					
					rowColRows[r][c] = splitIntoRows(rowCols[r][c], false, 0);
					
					rowColRowCols[r][c] = new Rectangle[rowColRows[r][c].length][];
					
					for (int s = 0; s < rowColRows[r][c].length; s++) {
						rowColRows[r][c][s] = narrowCol(rowColRows[r][c][s], false, 0);
						rowColRows[r][c][s] = narrowRow(rowColRows[r][c][s], false, 0);
						
						rowColRowCols[r][c][s] = splitIntoCols(rowColRows[r][c][s], false, 0);
						
						for (int d = 0; d < rowColRowCols[r][c][s].length; d++) {
							rowColRowCols[r][c][s][d] = narrowRow(rowColRowCols[r][c][s][d], false, 0);
							rowColRowCols[r][c][s][d] = narrowCol(rowColRowCols[r][c][s][d], false, 0);
							eliminatable.add(rowColRowCols[r][c][s][d]);
						}
					}
				}
			}
		}
		
		else {
			Rectangle[] cols = splitIntoCols(rect, false, 0);
			Rectangle[][] colRows = new Rectangle[cols.length][];
			Rectangle[][][] colRowCols = new Rectangle[cols.length][][];
			Rectangle[][][][] colRowColRows = new Rectangle[cols.length][][][];
			
			for (int c = 0; c < cols.length; c++) {
				cols[c] = narrowRow(cols[c], false, 0);
				cols[c] = narrowCol(cols[c], false, 0);
				
				colRows[c] = splitIntoRows(cols[c], false, 0);
				
				colRowCols[c] = new Rectangle[colRows[c].length][];
				colRowColRows[c] = new Rectangle[colRows[c].length][][];
				
				for (int r = 0; r < colRows[c].length; r++) {
					colRows[c][r] = narrowCol(colRows[c][r], false, 0);
					colRows[c][r] = narrowRow(colRows[c][r], false, 0);
					
					colRowCols[c][r] = splitIntoCols(colRows[c][r], false, 0);
					
					colRowColRows[c][r] = new Rectangle[colRowCols[c][r].length][];
					
					for (int d = 0; d < colRowCols[c][r].length; d++) {
						colRowCols[c][r][d] = narrowRow(colRowCols[c][r][d], false, 0);
						colRowCols[c][r][d] = narrowCol(colRowCols[c][r][d], false, 0);
						
						colRowColRows[c][r][d] = splitIntoRows(colRowCols[c][r][d], false, 0);
						
						for (int s = 0; s < colRowColRows[c][r][d].length; s++) {
							colRowColRows[c][r][d][s] = narrowCol(colRowColRows[c][r][d][s], false, 0);
							colRowColRows[c][r][d][s] = narrowRow(colRowColRows[c][r][d][s], false, 0);
							eliminatable.add(colRowColRows[c][r][d][s]);
						}
					}
				}
			}
		}
		
		ArrayList eliminated = new ArrayList();
		int white = Color.WHITE.getRGB();
		for (int e = 0; e < eliminatable.size(); e++) {
			Rectangle elRect = ((Rectangle) eliminatable.get(e));
			float elBrightness = getAverageBrightness(elRect);
			if (
					((((elRect.rightCol - elRect.leftCol) * (elRect.bottomRow - elRect.topRow)) < 500)
					&&
					(elBrightness > 0.9f))
					||
					((elRect.rightCol - elRect.leftCol) <= (((elRect.bottomRow - elRect.topRow) < 30) ? 2 : 3))
					||
					((elRect.bottomRow - elRect.topRow) <= (((elRect.rightCol - elRect.leftCol) < 30) ? 2 : 3))
				) {
				for (int x = elRect.leftCol; x < elRect.rightCol; x++)
					for (int y = elRect.topRow; y < elRect.bottomRow; y++) {
						paint(x, y, white, elRect.image, false);
						elRect.brightness[x][y] = 1f;
//						elRect.isWhite[x][y] = true;
					}
				eliminated.add(elRect);
			}
		}
		
		return ((Rectangle[]) eliminated.toArray(new Rectangle[eliminated.size()]));
	}
	
	static float getAverageBrightness(Rectangle rect) {
		float brightnessSum = 0f;
		for (int r = rect.topRow; r < rect.bottomRow; r++) {
			for (int c = rect.leftCol; c < rect.rightCol; c++)
				brightnessSum += rect.brightness[c][r];
		}
		return (brightnessSum / ((rect.bottomRow - rect.topRow) * (rect.rightCol - rect.leftCol)));
	}
	
	static Rectangle[] joinCols(Rectangle[] cols) {
		if (cols.length <= 1) return cols;
		
		Rectangle[] sorted = new Rectangle[cols.length];
		System.arraycopy(cols, 0, sorted, 0, cols.length);
		Arrays.sort(sorted, new Comparator() {
			public int compare(Object r1, Object r2) {
				return (((Rectangle) r1).leftCol - ((Rectangle) r2).leftCol);
			}
		});
		cols = sorted;
		
		int topRow = cols[0].topRow;
		int bottomRow = cols[0].bottomRow;
		
		for (int c = 1; c < cols.length; c++) {
			topRow = Math.min(topRow, cols[c].topRow);
			bottomRow = Math.max(bottomRow, cols[c].bottomRow);
		}
		
		System.out.println("Height: " + (bottomRow - topRow));
		
//		int maxDist = 0;
//		for (int c = 1; c < cols.length; c++) {
//			if (maxDist < (cols[c].leftCol - cols[c-1].rightCol))
//				maxDist = (cols[c].leftCol - cols[c-1].rightCol);
//		}
//		
//		int[] distBuckets = new int[maxDist + 1];
//		for (int c = 1; c < cols.length; c++)
//			distBuckets[cols[c].leftCol - cols[c-1].rightCol]++;
//		System.out.print(maxDist + " -->");
//		for (int b = 0; b < distBuckets.length; b++)
//			System.out.print(" " + distBuckets[b] + " -");
//		int joinDist = 1;//findBulkCenter(distBuckets, 3);
////		if (joinDist == 0)
////			joinDist = findBulkCenter(distBuckets, 0);
//		while ((joinDist < distBuckets.length) && (distBuckets[joinDist] == 0))
//			joinDist++;
//		while ((joinDist < distBuckets.length) && (distBuckets[joinDist] != 0))
//			joinDist++;
////		while (((joinDist + 1) < distBuckets.length) && ((distBuckets[joinDist] != 0) || (distBuckets[joinDist + 1] != 0)))
////			joinDist++;
//		if (((joinDist + 1) == distBuckets.length) && (distBuckets[joinDist] != 0))
//			joinDist++;
//		System.out.println("-> " + joinDist);
//		
//		if (((bottomRow - topRow) / 3) < joinDist)
//			joinDist = ((bottomRow - topRow) / 3);
//		else if (joinDist < ((bottomRow - topRow) / 10))
//			joinDist = ((bottomRow - topRow) / 3);
//		System.out.println("Join Distance: " + joinDist);
		
		Rectangle joint;
		ArrayList jointList = new ArrayList();
//		for (int c = 1; c < cols.length; c++) {
//			if ((cols[c].leftCol - joint.rightCol) <= joinDist) {
//				joint.topRow = Math.min(joint.topRow, cols[c].topRow);
//				joint.bottomRow = Math.max(joint.bottomRow, cols[c].bottomRow);
//				joint.leftCol = Math.min(joint.leftCol, cols[c].leftCol);
//				joint.rightCol = Math.max(joint.rightCol, cols[c].rightCol);
//			}
//			else {
//				jointList.add(joint);
//				joint = cols[c];
//			}
//		}
//		jointList.add(joint);
//		
//		cols = ((Rectangle[]) jointList.toArray(new Rectangle[jointList.size()]));
//		jointList.clear();
		
		for (int c = 1; c < (cols.length - 1); c++) {
			int leftDist = (cols[c].leftCol - cols[c-1].rightCol);
			int rightDist = (cols[c+1].leftCol - cols[c].rightCol);
			if ((((leftDist * 2) < rightDist) || (leftDist <= 3)) && (leftDist < ((bottomRow - topRow) / 3))) {
//				joint = new Rectangle(cols[c].image, cols[c].brightness, cols[c].isWhite);
				joint = new Rectangle(cols[c].image, cols[c].brightness);
				joint.topRow = Math.min(cols[c-1].topRow, cols[c].topRow);
				joint.bottomRow = Math.max(cols[c-1].bottomRow, cols[c].bottomRow);
				joint.leftCol = Math.min(cols[c-1].leftCol, cols[c].leftCol);
				joint.rightCol = Math.max(cols[c-1].rightCol, cols[c].rightCol);
				cols[c] = joint;
				cols[c-1] = null;
			}
			else if (((rightDist * 2) < leftDist) && (rightDist < ((bottomRow - topRow) / 3))) {
//				joint = new Rectangle(cols[c].image, cols[c].brightness, cols[c].isWhite);
				joint = new Rectangle(cols[c].image, cols[c].brightness);
				joint.topRow = Math.min(cols[c+1].topRow, cols[c].topRow);
				joint.bottomRow = Math.max(cols[c+1].bottomRow, cols[c].bottomRow);
				joint.leftCol = Math.min(cols[c+1].leftCol, cols[c].leftCol);
				joint.rightCol = Math.max(cols[c+1].rightCol, cols[c].rightCol);
				cols[c+1] = joint;
				cols[c] = cols[c-1];
				cols[c-1] = null;
			}
		}
		
		for (int c = 0; c < cols.length; c++)
			if (cols[c] != null)
				jointList.add(cols[c]);
		cols = ((Rectangle[]) jointList.toArray(new Rectangle[jointList.size()]));
		jointList.clear();
		
		
		int maxDist = 0;
		for (int c = 1; c < cols.length; c++) {
			if (maxDist < (cols[c].leftCol - cols[c-1].rightCol))
				maxDist = (cols[c].leftCol - cols[c-1].rightCol);
		}
		
		int[] distBuckets = new int[maxDist + 1];
		for (int c = 1; c < cols.length; c++)
			distBuckets[cols[c].leftCol - cols[c-1].rightCol]++;
		System.out.print(maxDist + " -->");
		for (int b = 0; b < distBuckets.length; b++)
			System.out.print(" " + distBuckets[b] + " -");
		int joinDist = findBulkCenter(distBuckets, 3);
		joinDist--;
//		while ((joinDist > 0) && (distBuckets[joinDist] != 0))
//			joinDist--;
		while ((joinDist > 0) && ((distBuckets[joinDist] != 0) || (distBuckets[joinDist + 1] != 0)))
			joinDist--;
		while ((joinDist > 0) && (distBuckets[joinDist] == 0))
			joinDist--;
		System.out.println("-> " + joinDist);
		
		if (((bottomRow - topRow) / 3) < joinDist)
			joinDist = ((bottomRow - topRow) / 3);
//		else if (joinDist < ((bottomRow - topRow) / 10))
//			joinDist = ((bottomRow - topRow) / 10);
		System.out.println("Join Distance: " + joinDist);
		
//		joint = cols[0];
//		for (int c = 1; c < cols.length; c++) {
//			if ((cols[c].leftCol - joint.rightCol) <= joinDist) {
//				joint.topRow = Math.min(joint.topRow, cols[c].topRow);
//				joint.bottomRow = Math.max(joint.bottomRow, cols[c].bottomRow);
//				joint.leftCol = Math.min(joint.leftCol, cols[c].leftCol);
//				joint.rightCol = Math.max(joint.rightCol, cols[c].rightCol);
//			}
//			else {
//				jointList.add(joint);
//				joint = cols[c];
//			}
//		}
//		jointList.add(joint);
//		
//		cols = ((Rectangle[]) jointList.toArray(new Rectangle[jointList.size()]));
//		jointList.clear();
		
		return cols;
	}
	
	static void paintLineMetrics(Rectangle line, Color color) {
		System.out.println("Painting line metrics:");
		float[] rowBrightnesses = new float[line.bottomRow - line.topRow];
		for (int r = line.topRow; r < line.bottomRow; r++) {
//			Rectangle row = new Rectangle(line.image, line.brightness, line.isWhite);
			Rectangle row = new Rectangle(line.image, line.brightness);
			row.topRow = r;
			row.bottomRow = (r+1);
			row.leftCol = line.leftCol;
			row.rightCol = line.rightCol;
			rowBrightnesses[r - line.topRow] = getAverageBrightness(row);
		}
		
		int maxEnvironment = 4;
		int rgb = color.getRGB();
		for (int r = 0; r < rowBrightnesses.length; r++) {
			boolean isDarkest = true;
			float minDist = 1f;
			float maxDist = 0f;
			for (int e = Math.max(0, (r - maxEnvironment)); e < Math.min(rowBrightnesses.length, (r + maxEnvironment)); e++) {
				if (e != r) {
					if (rowBrightnesses[e] < rowBrightnesses[r]) isDarkest = false;
					else {
						minDist = Math.min(minDist, (rowBrightnesses[e] - rowBrightnesses[r]));
						maxDist = Math.max(maxDist, (rowBrightnesses[e] - rowBrightnesses[r]));
					}
				}
			}
			if (isDarkest) {
				System.out.println("  - painting " + r + " of " + rowBrightnesses.length + ", brightness " + rowBrightnesses[r] + ", max dist " + maxDist + ", min dist " + minDist);
				for (int c = line.leftCol; c < line.rightCol; c++)
					paint(c, (line.topRow + r), rgb, line.image, false);
			}
		}
	}
	
	static void paintRowPasses(Rectangle rect, Color color) {
		//	do "shivering line" search
		if (rect.leftCol == rect.rightCol) return;
		
		int testColDist = 20;
//		int testColWidth = Math.max(testColDist, ((rect.rightCol - rect.leftCol) / 20)) + 1;
		int testColWidth = (3 * testColDist) + 1;
		
		int[] testCols = new int[(rect.rightCol - rect.leftCol + testColDist - 1) / testColDist];
		testCols[0] = rect.leftCol;
		
		boolean[][] isLinePaintable = new boolean[testCols.length][rect.bottomRow - rect.topRow];
		
		for (int t = 1; t < testCols.length; t++) {
			testCols[t] = rect.leftCol + ((t * (rect.rightCol - rect.leftCol)) / testCols.length);
			for (int r = rect.topRow; r < rect.bottomRow; r++) {
				boolean canPaint = true;
				for (int c = Math.max(rect.leftCol, (testCols[t] - (testColWidth / 2))); canPaint && (c < Math.min(rect.rightCol, (testCols[t] + (testColWidth / 2)))); c++)
					canPaint = canPaint && (rect.brightness[c][r] == 1.0f);
				
				isLinePaintable[t][r - rect.topRow] = canPaint;
				
				//	fill first column as well
				if (t == 1) isLinePaintable[0][r - rect.topRow] = canPaint;
			}
		}
		
		System.out.print("Test cols (line width " + testColWidth + "):");
		for (int t = 0; t < testCols.length; t++)
			System.out.print(" " + testCols[t]);
		System.out.println();
		
		
//		int rgb = color.getRGB();
//		for (int t = 1; t < testCols.length; t++) {
//			for (int r = rect.topRow; r < rect.bottomRow; r++) {
//				if (isLinePaintable[t][r - rect.topRow]) {
//					for (int c = Math.max(rect.leftCol, (testCols[t] - (testColWidth / 2))); c < Math.min(rect.rightCol, (testCols[t] + (testColWidth / 2))); c++) {
//						paint(c, r, rgb, rect.image, false);
//					}
//				}
//			}
//		}
		
		//	paint middle of pass found in black (use recursion and backtracking)
		int blockStart = 0;
		int blockEnd = 0;
		ArrayList passes = new ArrayList();
		while (blockEnd < isLinePaintable[0].length) {
			
			//	find end of block
			while ((blockEnd < isLinePaintable[0].length) && isLinePaintable[0][blockEnd])
				blockEnd++;
			
			//	find passes starting with middle of current block
			ArrayList blockPasses = new ArrayList();
			int[] passPrefix = new int[testCols.length];
			
			//	try starting from middle of block
//			passPrefix[0] = ((blockStart + blockEnd) / 2);
//			System.out.println("  - searching pass from " + passPrefix[0]);
//			findPass(isLinePaintable, 1, passPrefix, blockPasses);
//			System.out.println("    ==> found " + blockPasses.size() + " passes");
//			for (int c = Math.max(rect.leftCol, (testCols[0] - (testColWidth / 2))); c < Math.min(rect.rightCol, (testCols[0] + (testColWidth / 2))); c++)
//				paint(c, (rect.topRow + passPrefix[0]), 0, rect.image, false);
			
			//	try starting from top and bottom of block
//			passPrefix[0] = blockStart;
//			System.out.println("  - searching pass from " + passPrefix[0]);
//			findPass(isLinePaintable, 1, passPrefix, blockPasses);
//			System.out.println("    ==> found " + blockPasses.size() + " passes");
//			passPrefix[0] = blockEnd;
//			System.out.println("  - searching pass from " + passPrefix[0]);
//			findPass(isLinePaintable, 1, passPrefix, blockPasses);
//			System.out.println("    ==> found " + blockPasses.size() + " passes");
			
			//	try starting from deepest point of block
			passPrefix[0] = blockStart;
			int maxIndent = 1;
			for (int r = blockStart; r < blockEnd; r++) {
				int indent = 1;
				boolean noStraight = false;
				for (int c = 1; c < testCols.length; c++) {
					if (isLinePaintable[c][r])
						indent++;
					else {
						noStraight = true;
						c = testCols.length;
					}
				}
				for (int c = (testCols.length-1); c > 0 ; c--) {
					if (isLinePaintable[c][r])
						indent++;
					else {
						noStraight = true;
						c = 0;
					}
				}
				if ((indent > maxIndent) && noStraight) {
					passPrefix[0] = r;
					maxIndent = indent;
				}
			}
			System.out.println("  - searching pass from " + passPrefix[0]);
			findPass(isLinePaintable, 1, passPrefix, blockPasses);
			System.out.println("    ==> found " + blockPasses.size() + " passes");
			for (int c = Math.max(rect.leftCol, (testCols[0] - (testColWidth / 2))); c < Math.min(rect.rightCol, (testCols[0] + (testColWidth / 2))); c++)
				paint(c, (rect.topRow + passPrefix[0]), color.getRGB(), rect.image, false);
			
			//	smoothen passes
			for (int p = 0; p < blockPasses.size(); p++)
				smoothenPass(((int[]) blockPasses.get(p)), isLinePaintable, passPrefix[0]);
			
			//	eliminate marginally different passes
//			clusterPasses(blockPasses, isLinePaintable);
			
			System.out.println("    ==> retained " + blockPasses.size() + " passes");
			passes.addAll(blockPasses);
			
			//	proceed to next block
			blockStart = blockEnd;
			while ((blockStart < isLinePaintable[0].length) && !isLinePaintable[0][blockStart])
				blockStart++;
			blockEnd = blockStart;
		}
		
		//	clean up globally
//		for (int p = 0; p < passes.size(); p++)
//			smoothenPass(((int[]) passes.get(p)), isLinePaintable, -1);
		clusterPasses(passes, isLinePaintable);
		
		//	smooth passes to their middle until nothing changes any more
		for (int p = 0; p < passes.size(); p++) {
			int[] pass = ((int[]) passes.get(p));
			int pSum = -1;
			int pSumOld;
			do {
				pSumOld = pSum;
				smoothenPass(pass, isLinePaintable, -1);
				pSum = 0;
				for (int c = 0; c < pass.length; c++)
					pSum += pass[c];
			} while (pSum != pSumOld);
			
			pSum = -1;
			do {
				pSumOld = pSum;
				int min = isLinePaintable[0].length;
				for (int c = 0; c < pass.length; c++)
					if (pass[c] < min)
						min = pass[c];
				smoothenPass(pass, isLinePaintable, min);
				pSum = 0;
				for (int c = 0; c < pass.length; c++)
					pSum += pass[c];
			} while (pSum != pSumOld);
			
			pSum = -1;
			do {
				pSumOld = pSum;
				smoothenPass(pass, isLinePaintable, -1);
				pSum = 0;
				for (int c = 0; c < pass.length; c++)
					pSum += pass[c];
			} while (pSum != pSumOld);
			
			pSum = -1;
			do {
				pSumOld = pSum;
				int max = isLinePaintable[0].length;
				for (int c = 0; c < pass.length; c++)
					if (pass[c] > max)
						max = pass[c];
				smoothenPass(pass, isLinePaintable, max);
				pSum = 0;
				for (int c = 0; c < pass.length; c++)
					pSum += pass[c];
			} while (pSum != pSumOld);
			
			pSum = -1;
			do {
				pSumOld = pSum;
				smoothenPass(pass, isLinePaintable, -1);
				pSum = 0;
				for (int c = 0; c < pass.length; c++)
					pSum += pass[c];
			} while (pSum != pSumOld);
		}
		
		//	paint passes
		for (int p = 0; p < passes.size(); p++) {
			int[] pass = ((int[]) passes.get(p));
//			smoothenPass(pass, isLinePaintable, -1);
			for (int t = 1; t < testCols.length; t++)
				paintLine(testCols[t-1], (rect.topRow + pass[t-1]), testCols[t], (rect.topRow + pass[t]), color.getRGB(), rect.image, false);
			paintLine(testCols[testCols.length-1], (rect.topRow + pass[testCols.length-1]), rect.rightCol, (rect.topRow + pass[testCols.length-1]), color.getRGB(), rect.image, false);
		}
	}
	
	/*
	 * passIndex >= 1
	 * passIndex <= passCols.length
	 * passPrefix filled up to (passIndex-1), inclusive
	 */
	static void findPass(boolean[][] passable, int passIndex, int[] passPrefix, ArrayList passes) {
		
		//	end of recursion
		if (passIndex == passPrefix.length) {
			int[] pass = new int[passPrefix.length];
			System.arraycopy(passPrefix, 0, pass, 0, pass.length);
			passes.add(pass);
			return;
		}
		
		//	find top and bottom of pass in previous column
		int preTop = passPrefix[passIndex -1];
		while ((preTop != 0) && passable[passIndex-1][preTop-1])
			preTop--;
		
		int preBottom = passPrefix[passIndex -1];
		while ((preBottom < passable[passIndex-1].length) && passable[passIndex-1][preBottom])
			preBottom++;
		
		//	find top and bottom of possible pass extensions in current column
		int top = preTop;
		while ((top != 0) && passable[passIndex][top-1])
			top--;
		while ((top < passable[passIndex].length) && !passable[passIndex][top])
			top++;
		
		int bottom = preBottom;
		while ((bottom < passable[passIndex].length) && passable[passIndex][bottom])
			bottom++;
		while ((bottom != 0) && !passable[passIndex][bottom-1])
			bottom--;
		
		//	extend pass
		int blockStart = top;
		int blockEnd = top;
		while (blockEnd < bottom) {
			
			//	find end of block (current column might be split in the middle)
			while ((blockEnd < bottom) && passable[passIndex][blockEnd])
				blockEnd++;
			
			//	extend pass with middle of current block
//			passPrefix[passIndex] = ((blockStart + blockEnd) / 2);
			passPrefix[passIndex] = Math.max(blockStart, Math.min(passPrefix[passIndex -1], blockEnd));
			findPass(passable, (passIndex+1), passPrefix, passes);
			
			//	proceed to next block
			blockStart = blockEnd;
			while ((blockStart < bottom) && !passable[passIndex][blockStart])
				blockStart++;
			blockEnd = blockStart;
		}
	}
	
	static void clusterPasses(ArrayList passes, boolean[][] passeable) {
		if (passes.isEmpty()) return;
		
		int psCount;
		
		int[][] pss = ((int[][]) passes.toArray(new int[passes.size()][]));
		int middle = 0;
		for (int p = 0; p < pss.length; p++) {
			for (int c = 0; c < pss[p].length; c++)
				middle += pss[p][c];
		}
		middle /= (pss.length * pss[0].length);
		
		int[] psFuzzyness = new int[pss.length];
		for (int p = 0; p < pss.length; p++) {
			psFuzzyness[p] = 0;
			for (int c = 1; c < pss[p].length; c++)
				psFuzzyness[p] += Math.abs(pss[p][c] - pss[p][c-1]);
		}
		
		int[] psDeviation = new int[pss.length];
		for (int p = 0; p < pss.length; p++) {
			psDeviation[p] = 0;
			for (int c = 0; c < pss[p].length; c++)
				psDeviation[p] += Math.abs(pss[p][c] - middle);
		}
		
		int[] psWidth = new int[pss.length];
		int[][] psWidthDetails = new int[pss.length][];
		for (int p = 0; p < pss.length; p++) {
			psWidth[p] = 0;
			psWidthDetails[p] = new int[pss[p].length];
			for (int c = 0; c < pss[p].length; c++) {
				psWidthDetails[p][c] = 0;
				int n = pss[p][c];
				while ((n != 0) && passeable[c][n-1]) {
					n--;
					psWidth[p]++;
					psWidthDetails[p][c]++;
				}
				n = pss[p][c];
				while ((n < passeable[c].length) && passeable[c][n]) {
					n++;
					psWidth[p]++;
					psWidthDetails[p][c]++;
				}
			}
		}
		int psLimit = pss.length;
		
		do {
			psCount = psLimit;
			
			for (int p = 0; p < psLimit; p++) {
				for (int q = (p+1); q < psLimit; q++) {
					int diff = 0;
					int dist = 0;
					for (int c = 0; c < pss[p].length; c++) {
						if (pss[p][c] != pss[q][c]) {
							diff++;
							dist += Math.abs(pss[p][c] - pss[q][c]);
						}
					}
					
					boolean eliminate = false;
					
					//	passes share at least half of the points
					if (diff < (pss[p].length / 2)) {
						eliminate = true;
					}
					
//					//	passes are less than three pixels apart on average
//					if (dist < (3 * pss[p].length)) {
//						eliminate = true;
//					}
					
					if (eliminate) {
						boolean qBetter = false;
						
						//	pass q has less sharp bends than pass p
						if (psFuzzyness[q] < psFuzzyness[p])
							qBetter = true;
						
//						//	pass q has less deviation from the middle than pass p
//						if (psDeviation[q] < psDeviation[p])
//							qBetter = true;
						
//						//	pass q has more space around it than pass p
//						if (psWidth[q] >= psWidth[p])
//							qBetter = true;
						
						
						if (qBetter) {
							pss[p] = pss[q];
							psFuzzyness[p] = psFuzzyness[q];
							psDeviation[p] = psDeviation[q];
							psWidth[p] = psWidth[q];
						}
						for (int c = q; c < psLimit; c++) {
							pss[c] = (((c+1) == psLimit) ? null : pss[c+1]);
							psFuzzyness[c] = (((c+1) == psLimit) ? -1 : psFuzzyness[c+1]);
							psDeviation[c] = (((c+1) == psLimit) ? -1 : psDeviation[c+1]);
							psWidth[c] = (((c+1) == psLimit) ? -1 : psWidth[c+1]);
						}
						q--;
						psLimit--;
					}
				}
			}
			
		} while (psLimit < psCount);
		
		passes.clear();
		for (int p = 0; p < psLimit; p++)
			passes.add(pss[p]);
	}
	
	static void smoothenPass(int[] pass, boolean[][] passable, int middle) {
		
		//	compute average (if not prescribed)
		if (middle < 0) {
			middle = 0;
			for (int c = 0; c < pass.length; c++)
				middle += pass[c];
			middle /= pass.length;
		}
		
		//	flatten pass (draw all points as close to the middle as possible
		for (int c = 0; c < pass.length; c++) {
			while ((pass[c] < middle) && ((pass[c] + 1) < passable[c].length) && passable[c][pass[c]+1])
				pass[c]++;
			while ((pass[c] > middle) && (pass[c] != 0) && passable[c][pass[c]-1])
				pass[c]--;
		}
		
		//	check if pass is ascending or descending TODO: make use of this somehow
		
		//	smothen out ditches
	}
	
	static void paintLine(int x1, int y1, int x2, int y2, int rgb, BufferedImage image, boolean mix) {
		if (Math.abs(x1 - x2) < Math.abs(y1 - y2)) {
			int x_1;
			int x_2;
			int t;
			int b;
			if (y1 < y2) {
				x_1 = x1;
				x_2 = x2;
				t = y1;
				b = y2;
			}
			else {
				x_1 = x2;
				x_2 = x1;
				t = y2;
				b = y1;
			}
			for (int y = t; y <= b; y++) {
				int x = (((y - t) * x_2) + ((b - y) * x_1)) / (b - t);
				paint(x, y, rgb, image, mix);
			}
		}
		else {
			int l;
			int r;
			int y_1;
			int y_2;
			if (x1 < x2) {
				l = x1;
				r = x2;
				y_1 = y1;
				y_2 = y2;
			}
			else {
				l = x2;
				r = x1;
				y_1 = y2;
				y_2 = y1;
			}
			for (int x = l; x <= r; x++) {
				int y = (((x - l) * y_2) + ((r - x) * y_1)) / (r - l);
				paint(x, y, rgb, image, mix);
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
//		File imageFile = new File("E:/Projektdaten/test_staple/5834_Page_2.jpg");
//		File imageFile = new File("E:/Projektdaten/Zootaxa_staple/21825_Seite_13.jpg");
//		File imageFile = new File("E:/Projektdaten/test_staple/5834_Page_2_dark.jpg");
		File imageFile = new File("E:/GoldenGATEv3.Server/Temp-Dev/77BEDDA13EA2D5AF625160E4E1A842D5_Seite_04.png");
		
		//	TODO: also check if bucketization to a short[][] or even a byte[][] might be sufficiently fine grained
		
		InputStream is = new FileInputStream(imageFile);
		BufferedImage image = ImageIO.read(is);
		is.close();
		System.out.println("   - image loaded, color model is " + image.getColorModel().getClass().getName());
		System.out.println("   - image type is " + image.getType());
		
		image = cloneImage(image);
		System.out.println("   - image cloned, type is " + image.getType());
		
		Rectangle rect = new Rectangle(image);
		whitenWhite(rect);
		
		rect = narrowCol(rect, false, 0);
		rect = narrowRow(rect, false, 0);
		
		
		ArrayList featherDustedList = new ArrayList();
		Set featherDustedRows = new HashSet();
		Rectangle[] featherDusted;
		
		featherDusted = featherDust(rect, true);
		featherDustedRows.addAll(Arrays.asList(featherDusted));
		featherDustedList.addAll(Arrays.asList(featherDusted));
		
		featherDusted = featherDust(rect, false);
		featherDustedList.addAll(Arrays.asList(featherDusted));
		
		
		Rectangle[] cols;
		Rectangle[][] colRows;
		Rectangle[][][] colRowCols;
//		Rectangle[][][][] colRowColRows;
		
		cols = splitIntoCols(rect, false, 0);
		colRows = new Rectangle[cols.length][];
		colRowCols = new Rectangle[cols.length][][];
//		colRowColRows = new Rectangle[cols.length][][][];
		
		for (int c = 0; c < cols.length; c++) {
			cols[c] = narrowRow(cols[c], false, 0);
			cols[c] = narrowCol(cols[c], false, 0);
			
			colRows[c] = splitIntoRows(cols[c], false, 0);
			
			for (int r = 0; r < colRows[c].length; r++) {
				colRows[c][r] = narrowCol(colRows[c][r], false, 0);
				colRows[c][r] = narrowRow(colRows[c][r], false, 0);
			}
			
//			TODO: use this
//			colRows[c] = smoothenRowSplit(colRows[c], 1.0f);
			
			colRowCols[c] = new Rectangle[colRows[c].length][];
//			colRowColRows[c] = new Rectangle[colRows[c].length][][];
			
			for (int r = 0; r < colRows[c].length; r++) {
				colRows[c][r] = narrowCol(colRows[c][r], false, 0);
				colRows[c][r] = narrowRow(colRows[c][r], false, 0);
				
				colRowCols[c][r] = splitIntoCols(colRows[c][r], false, 0);
				
//				colRowColRows[c][r] = new Rectangle[colRowCols[c][r].length][];
				
				for (int d = 0; d < colRowCols[c][r].length; d++) {
					colRowCols[c][r][d] = narrowRow(colRowCols[c][r][d], false, 0);
					colRowCols[c][r][d] = narrowCol(colRowCols[c][r][d], false, 0);
					
					whitenWhite(colRowCols[c][r][d]);
					
					colRowCols[c][r][d] = narrowRow(colRowCols[c][r][d], false, 0);
					colRowCols[c][r][d] = narrowCol(colRowCols[c][r][d], false, 0);
					
//					paintRect(colRowCols[c][r][d], Color.BLUE, true);
//					
//					colRowColRows[c][r][d] = splitIntoRows(colRowCols[c][r][d], false, 0);
//					
//					for (int s = 0; s < colRowColRows[c][r][d].length; s++) {
//						colRowColRows[c][r][d][s] = narrowCol(colRowColRows[c][r][d][s], false, 0);
//						colRowColRows[c][r][d][s] = narrowRow(colRowColRows[c][r][d][s], false, 0);
//					}
				}
				
//				TODO: use this
//				colRowCols[c][r] = joinCols(colRowCols[c][r]);
			}
		}
		
//		for (int c = 0; c < cols.length; c++) {
//			for (int r = 0; r < colRows[c].length; r++) {
//				paintLineMetrics(colRows[c][r], Color.RED);
//			}
//		}
		
//		for (int c = 0; c < cols.length; c++) {
//			for (int r = 0; r < colRows[c].length; r++) {
//				paintIndents(colRows[c][r], Color.YELLOW, Color.PINK, 1f, 100);
//				paintRowPasses(colRows[c][r], Color.CYAN);
//			}
//		}
		
		for (int c = 0; c < cols.length; c++) {
			for (int r = 0; r < colRows[c].length; r++) {
				for (int d = 0; d < colRowCols[c][r].length; d++) {
//					for (int s = 0; s < colRowColRows[c][r][d].length; s++) {
//						paintRect(colRowColRows[c][r][d][s], Color.BLUE, true);
//					}
//					paintRect(colRowCols[c][r][d], Color.RED, false);
				}
				paintRect(colRows[c][r], Color.GREEN, true);
			}
//			paintRect(cols[c], Color.RED, true);
		}
		
//		for (int fd = 0; fd < featherDustedList.size(); fd++) {
//			Rectangle fdRect = ((Rectangle) featherDustedList.get(fd));
//			paintRect(fdRect, (featherDustedRows.contains(fdRect) ? Color.CYAN : Color.BLUE), false);
//		}
		
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
		
		float[] sorted = new float[data.length];
		System.arraycopy(data, 0, sorted, 0, data.length);
		Arrays.sort(sorted);
		
		float pivot = sorted[sorted.length - 1 - offset];
		return pivot;
	}
	
	static boolean isPainteable(float minDiff, float maxDiff) {
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
