package math.dwt;

import java.text.DecimalFormat;

import math.constants.FileNaming;
import math.dwt.wavelets.HaarAdaptive;

public class DWT {
	private Wavelet2DTransformation tranformation;
	public Wavelet2DTransformation getTranformation(){return tranformation;} 
	public DWT(Wavelet2DTransformation tranformation) {
		super();
		this.tranformation = tranformation;
	}
	
	public DWTCoefficients decompose(Matrix inputMatrix, boolean calculateMatrixNorms, String fileSaveName){
//		 = new Matrix(input);
//		System.out.println("Input matrix = " + inputMatrix);
		
		final int rows = inputMatrix.getRowsCount();
		final int columns = inputMatrix.getColumnsCount();
		final int coefRows = Math.round(rows/2);
		final int coefColumns = Math.round(columns/2);
		Matrix ma,mv,mh,md;
		ma = new Matrix(coefRows,coefColumns);
		mv = new Matrix(coefRows,coefColumns);
		mh = new Matrix(coefRows,coefColumns);
		md = new Matrix(coefRows,coefColumns);
				
//		System.out.println("DWT is processing "+fileSaveName+". Transform = "+tranformation.getCaption());
		Matrix adoptiveMap = null;
		if (tranformation instanceof HaarAdaptive){
			adoptiveMap = new Matrix(coefRows,coefColumns);
		}
		processCoeficients(inputMatrix,ma,mv,mh,md,adoptiveMap);
		DWTCoefficients DWTCoefs = new DWTCoefficients(ma, mv, mh, md, adoptiveMap, calculateMatrixNorms);

		DecimalFormat myFormatter = new DecimalFormat("#,000");
		
		System.out.println(fileSaveName+
				(DWTCoefs.getNormMh())+
				"\t"+(DWTCoefs.getNormMv())+
				"\t"+(DWTCoefs.getNormMd())+
				"\t\t"+myFormatter.format(DWTCoefs.getNormMa())+
				"\t\tV,H,D Sum: "+myFormatter.format(DWTCoefs.getNormVHDSum())
				);
		if (fileSaveName!=null && fileSaveName != ""){
			ma.saveToFile(fileSaveName+tranformation.getCaption()+FileNaming.mAverageCoef+FileNaming.ext,	"Average coefs "+fileSaveName);
			mh.saveToFile(fileSaveName+tranformation.getCaption()+FileNaming.mHorizCoef+FileNaming.ext, 	"Horiz coefs "+fileSaveName);
			mv.saveToFile(fileSaveName+tranformation.getCaption()+FileNaming.mVerticalCoef+FileNaming.ext, 	"Vert coefs "+fileSaveName);
			md.saveToFile(fileSaveName+tranformation.getCaption()+FileNaming.mDialonalCoef+FileNaming.ext, 	"Diag coefs "+fileSaveName);
			if (adoptiveMap!=null)
				adoptiveMap.saveToFile(fileSaveName+tranformation.getCaption()+FileNaming.mTransfMap+FileNaming.ext, "Transformation mapping "+fileSaveName);
		}
		return DWTCoefs;
	}

//	private float[] dwtCoef;
	/**
	 * ma-md are half a size of inputMatrix
	 * 
	 * @param inputMatrix Color matrix 
	 * @param ma Average Matrix
	 * @param mv Vertical
	 * @param mh Horiz
	 * @param md Diag
	 * @param map transf map for Adoptive Haar
	 */
	private void processCoeficients(Matrix inputMatrix, Matrix ma, Matrix mv, Matrix mh, Matrix md, Matrix map) {
		final int rows = inputMatrix.getRowsCount();
		final int columns = inputMatrix.getColumnsCount();
		
		float[] dwtCoef = null;
		if (map!=null){
			for (int i = 0; i < rows; i+=2){
				for (int j = 0; j < columns; j+=2){
					dwtCoef = tranformation.perform(
							new float[]{
								inputMatrix.get()[i][j], 
								inputMatrix.get()[i][j+1], 
								inputMatrix.get()[i+1][j], 
								inputMatrix.get()[i+1][j+1]
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
					dwtCoef = tranformation.perform(
							new float[]{
								inputMatrix.get()[i][j], 
								inputMatrix.get()[i][j+1], 
								inputMatrix.get()[i+1][j], 
								inputMatrix.get()[i+1][j+1]
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
		final int rows = coefs.getMa().getRowsCount(); 
		final int columns = coefs.getMa().getColumnsCount();
		Matrix reconstructedMatrix = new Matrix(rows*2, columns*2); 
		
		float [] dwtCoef = null;
		if (tranformation instanceof HaarAdaptive){
			try {
				HaarAdaptive haarAdaptive = (HaarAdaptive) tranformation;
				float [][] transfMap = coefs.getMap().get();
				
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
				System.err.println("DWT reconstruct. Wrong parameters for Adaptive Haar?\n" + e.getMessage());
			} catch (Exception e) {
				System.err.println("DWT reconstruct for Adaptive Haar failed.\n" + e.getMessage());
			}
		} else {
			for (int i = 0; i < rows; i++){
				for (int j = 0; j < columns; j++){
					dwtCoef = tranformation.inverse(
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

}
