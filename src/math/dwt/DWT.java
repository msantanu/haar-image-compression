package math.dwt;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import math.dwt.wavelets.*;
import math.utils.FileNamesConst;
import math.utils.Log;

public class DWT {
	private Wavelet2DTransformation mTranformation;
	public Wavelet2DTransformation getTranformation(){return mTranformation;} 
	public DWT(Wavelet2DTransformation tranformation) {
		super();
		this.mTranformation = tranformation;
	}
	
	/**
	 * Decompose the given matrix 
	 * @param inputMatrixes			matrix array to decompose
	 * @param calculateMatrixNorms 	calculate norm of output matixes
	 * @param logCoefsToFile		to log out matixes
	 * @param level 				decomposition level
	 * @return
	 */
	public DWTCoefficients[] decompose(Matrix [] inputMatrixes, boolean calculateMatrixNorms, boolean logCoefsToFile, int level){
		return new DWTCoefficients[] {
				decompose(inputMatrixes[0], true, logCoefsToFile?"red":""	, level),
				decompose(inputMatrixes[1], true, logCoefsToFile?"green":""	, level),
				decompose(inputMatrixes[2], true, logCoefsToFile?"blue":""	, level)
		};
	}
	/**
	 * Decompose the given matrix 
	 * *recursive
	 * @param inputMatrix			matrix to decompose
	 * @param calculateMatrixNorms 	calculate norm of output matixes
	 * @param fileSaveName			filename to log out matixes
	 * @param level 				decomposition level
	 * @return
	 */
	private DWTCoefficients decompose(Matrix inputMatrix, boolean calculateMatrixNorms, final String fileSaveName, int level){
		
		final int rows = inputMatrix.getRowsCount();
		final int columns = inputMatrix.getColumnsCount();
		final int coefRows = (rows+mTranformation.getLength()-1)/mTranformation.getLength();
		final int coefColumns = (columns+mTranformation.getLength()-1)/mTranformation.getLength();
		Matrix ma,mv,mh,md;
		ma = new Matrix(coefRows,coefColumns);
		mv = new Matrix(coefRows,coefColumns);
		mh = new Matrix(coefRows,coefColumns);
		md = new Matrix(coefRows,coefColumns);
		ma.setTransform(mTranformation); //init further composable coefs
				
//		System.out.println("DWT is processing "+fileSaveName+". Transform = "+tranformation.getCaption());
		Matrix adaptiveMap = null;
		if (mTranformation instanceof HaarAdaptive){
			adaptiveMap = new Matrix(coefRows,coefColumns);
		}
		doWaveletTranform(inputMatrix,ma,mv,mh,md,adaptiveMap);
		DWTCoefficients resDWTCoefs = new DWTCoefficients(
				(level>1)?decompose(ma, calculateMatrixNorms, fileSaveName, level-1):ma
				, mv, mh, md, adaptiveMap, calculateMatrixNorms);

		String adaptiveMapStatistic = null;
		if (adaptiveMap != null) {
			adaptiveMapStatistic = "";
			Map<Integer, Integer> matrixStatistics = getMatrixStatistics(adaptiveMap);

			try {
				adaptiveMapStatistic += "HaarClassic " + matrixStatistics.get(0) + " (" +(matrixStatistics.get(0)*100/matrixStatistics.get(-1))+"%)";
				adaptiveMapStatistic += "; HaarVertical " + matrixStatistics.get(1) + " (" +(matrixStatistics.get(1)*100/matrixStatistics.get(-1))+"%)";
				adaptiveMapStatistic += "; HaarHorizotal " + matrixStatistics.get(2) + " (" +(matrixStatistics.get(2)*100/matrixStatistics.get(-1))+"%)";
				adaptiveMapStatistic += "; HaarDiagonal " + matrixStatistics.get(3) + " (" +(matrixStatistics.get(3)*100/matrixStatistics.get(-1))+"%)";
			} catch (NullPointerException e) {
				Log.getInstance().log(Level.WARNING, "NPE:: DWT.decompose(), matrixStatistics...");
			}
		}
		
		//output decomposition coefficients
		DecimalFormat myFormatter = new DecimalFormat("#,000");
		Log.getInstance().log(Level.FINE, 
				fileSaveName+"L"+level+"\t"+
				(resDWTCoefs.getNormMh())+
				"\t"+(resDWTCoefs.getNormMv())+
				"\t"+(resDWTCoefs.getNormMd())+
				"\t\t"+myFormatter.format(resDWTCoefs.getNormMa())+
				"\t\tV,H,D Sum: "+myFormatter.format(resDWTCoefs.getNormVHDSum())+
				(adaptiveMapStatistic!=null?"\t"+adaptiveMapStatistic:"")
				);
		if (fileSaveName!=null && fileSaveName != ""){
			ma.saveToFile(fileSaveName+mTranformation.getCaption()+"Lvl"+level+FileNamesConst.mAverageCoef+FileNamesConst.extData,	"Average coefs "+fileSaveName);
			mh.saveToFile(fileSaveName+mTranformation.getCaption()+"Lvl"+level+FileNamesConst.mHorizCoef+FileNamesConst.extData, 	"Horiz coefs "+fileSaveName);
			mv.saveToFile(fileSaveName+mTranformation.getCaption()+"Lvl"+level+FileNamesConst.mVerticalCoef+FileNamesConst.extData, 	"Vert coefs "+fileSaveName);
			md.saveToFile(fileSaveName+mTranformation.getCaption()+"Lvl"+level+FileNamesConst.mDialonalCoef+FileNamesConst.extData, 	"Diag coefs "+fileSaveName);
			if (adaptiveMap!=null)
				adaptiveMap.saveToFile(fileSaveName+mTranformation.getCaption()+FileNamesConst.mTransfMap+FileNamesConst.extData, "Transformation mapping "+fileSaveName);
		}
		return resDWTCoefs;
	}

//	private float[] dwtCoef;
	/**
	 * Fill the decomposition matrixes ma-md by decomposing the inputMatrix 
	 * ma-md are half a size of inputMatrix
	 * 
	 * @param inputMatrix to decompose 
	 * @param ma Average coefs matrix
	 * @param mv Vertical
	 * @param mh Horiz
	 * @param md Diag
	 * @param map transf map for Adaptive Haar
	 */
	private void doWaveletTranform(Matrix inputMatrix, Matrix ma, Matrix mv, Matrix mh, Matrix md, Matrix map) {
		int rows = inputMatrix.getRowsCount();
		int columns = inputMatrix.getColumnsCount();
		
		float[] dwtCoef = null;
		if (map!=null){
			for (int i = 0; i < rows; i+=2){
				for (int j = 0; j < columns; j+=2){
					dwtCoef = mTranformation.perform(
							new float[]{
								inputMatrix.get()[i][j], 
//								inputMatrix.get()[i][j+1], 
//								inputMatrix.get()[i+1][j], 
//								inputMatrix.get()[i+1][j+1]
								inputMatrix.get(i, j+1),
								inputMatrix.get(i+1, j),
								inputMatrix.get(i+1, j+1),
								}
							);
					ma.set(i/2,j/2,dwtCoef[0]);
					mv.set(i/2,j/2,dwtCoef[1]);
					mh.set(i/2,j/2,dwtCoef[2]);
					md.set(i/2,j/2,dwtCoef[3]);
					
					//transformations map
					map.set(i/2,j/2,dwtCoef[4]);
				}
			}
		} else {
			for (int i = 0; i < rows; i+=2){
				for (int j = 0; j < columns; j+=2){
					dwtCoef = mTranformation.perform(
							new float[]{
								inputMatrix.get()[i][j], 
//								inputMatrix.get()[i][j+1], 
//								inputMatrix.get()[i+1][j], 
//								inputMatrix.get()[i+1][j+1]
								inputMatrix.get(i, j+1),
								inputMatrix.get(i+1, j),
								inputMatrix.get(i+1, j+1),
								}
							);
					ma.set(i/2,j/2,dwtCoef[0]);
					mv.set(i/2,j/2,dwtCoef[1]);
					mh.set(i/2,j/2,dwtCoef[2]);
					md.set(i/2,j/2,dwtCoef[3]);
				}
			}
		}
	}
	
	public Matrix reconstruct(DWTCoefficients coefs){
		float [][] ma, mv, mh, md;
		ma = coefs.getMa().get();	
		mv = coefs.getMv().get();	
		mh = coefs.getMh().get();	
		md = coefs.getMd().get();
		int rows = mv.length; 
		int columns = mv[0].length;
		Log.getInstance().log(Level.FINEST, "DWT.reconstruct(), " +
				"ma ["+ma.length+", "+ma[0].length+"], " +
				"mv ["+mv.length+", "+mv[0].length+"], " + 
				"mh ["+mh.length+", "+mh[0].length+"], " + 
				"md ["+md.length+", "+md[0].length+"]."
				);
		Matrix reconstructedMatrix = new Matrix(rows*2, columns*2); 
		reconstructedMatrix.setTransform(mTranformation);
		
		float [] dwtCoef = null;
		if (mTranformation instanceof HaarAdaptive){
			try {
				HaarAdaptive haarAdaptive = (HaarAdaptive) mTranformation;
				float [][] transfMap = coefs.getMap().get();
				
				rows = transfMap.length; 
				columns = transfMap[0].length;
				
				for (int i = 0; i < rows; i++){
					for (int j = 0; j < columns; j++){
						dwtCoef = haarAdaptive.inverse(
								new float[] {
									ma[i][j],
									mv[i][j],
									mh[i][j],
									md[i][j],
									transfMap[i][j],
								}
							);
						reconstructedMatrix.set(i*2, 	j*2, 	dwtCoef[0]);
						reconstructedMatrix.set(i*2,	j*2+1,	dwtCoef[1]);
						reconstructedMatrix.set(i*2+1, 	j*2,	dwtCoef[2]);
						reconstructedMatrix.set(i*2+1, 	j*2+1, 	dwtCoef[3]);
					}	
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				System.err.println("DWT reconstruct. Wrong array index for Adaptive Haar: " + e.getMessage());
				Log.getInstance().log(Level.SEVERE, String.format("ArrayIndexOutOfBoundsException: column = %d, row = %d, error = %s", columns, rows, e.getMessage().toString()));
			} catch (Exception e1) {
				System.err.println("DWT reconstruct for Adaptive Haar failed.\nError: " + e1 );
			}
		} else {
			for (int i = 0; i < rows; i++){
				for (int j = 0; j < columns; j++){
					dwtCoef = mTranformation.inverse(
							new float[] {
								ma[i][j],
								mv[i][j],
								mh[i][j],
								md[i][j],
							}
						);
					reconstructedMatrix.set(i*2, 	j*2, 	dwtCoef[0]);
					reconstructedMatrix.set(i*2,	j*2+1,	dwtCoef[1]);
					reconstructedMatrix.set(i*2+1, 	j*2,	dwtCoef[2]);
					reconstructedMatrix.set(i*2+1, 	j*2+1, 	dwtCoef[3]);
				}	
			}
		}
		
		
		return reconstructedMatrix;
	}
	
	private Map<Integer, Integer> getMatrixStatistics(Matrix m) {
		float[][] values = m.get();
		Map<Integer, Integer> res = new HashMap<Integer, Integer>();
		int val;
		Integer count;
		int total = 0;
		for (float[] row : values)
			for (Float j : row) {
				res.put((val = (int)j.floatValue()), ((count = res.get(val)) == null ? 1 : count.intValue()+1));
				total++;
			}
		res.put(-1, total);
		return res;
	}


}
