package math.dwt;

import math.constants.FileNaming;
import math.dwt.wavelets.HaarClassic;

public class DWT {
	private Wavelet2DTransformation tranformation;
	public DWT(Wavelet2DTransformation tranformation) {
		super();
		this.tranformation = tranformation;
	}

	public DWTCoefficients calculate(Matrix inputMatrix, boolean calculateMatrixNorms, String fileSaveName){
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
		processCoeficients(inputMatrix,ma,mv,mh,md);
	
		DWTCoefficients dwtCoefficients = new DWTCoefficients(ma, mv, mh, md, calculateMatrixNorms);
		System.out.println(tranformation.getCaption()+" "+fileSaveName+" coeficients matrix norms:"+
				" horiz="+dwtCoefficients.getNormMh()+
				", vertical="+dwtCoefficients.getNormMv()+
				", diag="+dwtCoefficients.getNormMd());
//		System.out.println("\t"+dwtCoefficients.getNormMh()+
//				"\t"+dwtCoefficients.getNormMv()+
//				"\t"+dwtCoefficients.getNormMd());
		if (fileSaveName!=null && fileSaveName != ""){
			ma.saveToFile(fileSaveName+tranformation.getCaption()+FileNaming.mAverageCoef+FileNaming.ext,	"Average coefs "+fileSaveName);
			mh.saveToFile(fileSaveName+tranformation.getCaption()+FileNaming.mHorizCoef+FileNaming.ext, 	"Horiz coefs "+fileSaveName);
			mv.saveToFile(fileSaveName+tranformation.getCaption()+FileNaming.mVerticalCoef+FileNaming.ext, 	"Vert coefs "+fileSaveName);
			md.saveToFile(fileSaveName+tranformation.getCaption()+FileNaming.mDialonalCoef+FileNaming.ext, 	"Diag coefs "+fileSaveName);
		}
		return dwtCoefficients;
	}

	private void processCoeficients(Matrix inputMatrix, Matrix ma, Matrix mv, Matrix mh, Matrix md) {
		final int rows = inputMatrix.getRowsCount();
		final int columns = inputMatrix.getColumnsCount();
		
		HaarClassic haar = new HaarClassic();
		float c1,c2,c3,c4;
		for (int i = 0; i < rows; i+=2){
			for (int j = 0; j < columns; j+=2){
				float [] dwtCoef = tranformation.perform(
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
