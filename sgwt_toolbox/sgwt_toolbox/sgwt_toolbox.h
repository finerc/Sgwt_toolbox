#pragma once
#include "iostream"
#include "cstdio"
#include "vector"
#include "cmath"
#include "string"
#include "GN.h"
#include "Handle_sgwt_cheby_op.h"
#include <Eigen/Dense>
#include <Eigen/Sparse>
#include <Eigen/Core>
#include <Eigen/SparseCore>
#include <GenEigsSolver.h>
#include <MatOp/SparseGenMatProd.h>
using namespace std;
using namespace Spectra;
using namespace Eigen;

typedef Eigen::SparseMatrix<double> SpMat; // 声明一个列优先的双精度稀疏矩阵类型


typedef double(*G)(double x);
class Sgwt;

struct Varargin
{
	string designtype;
	double lpfactor;
	double a;
	double b;
	double t1;
	double t2;

	Varargin(string _designtype = "abspline3", double _lpfactor = 20, double _a = 2, double _b = 2, double _t1 = 1, double _t2 = 2) :designtype(_designtype), lpfactor(_lpfactor),
		a(_a), b(_b), t1(_t1), t2(_t2)
	{}
};

class G0 {
	double lminfac, gamma_1;
	G g1;
public:
	double operator()(double x)
	{
		x = x / lminfac;
		return gamma_1*g1(x);
	}

	G0(G g1, double lminfac, double gamma_1)
	{
		this->lminfac = lminfac;
		this->g1 = g1;
		this->gamma_1 = gamma_1;
	}
};

class Sgwt {
	const int m, Nscales;			
	VectorXd t;
	Varargin va; 
	SpMat L;
	GN *g;			//num =	Nscales
	G0 *g0;
	double arange[2];	
	vector<VectorXd> c;		//模拟cell

public:

	Sgwt(int _m,int _Nscales,SpMat _L);

	int getNscales()
	{
		return Nscales;
	}

	GN* getGN()
	{
		return g;
	}

	G0* getG0()
	{
		return g0;
	}

	vector<VectorXd>& getc()
	{
		return c;
	}

	static double sgwt_kernel_abspline3(double x);

	static double _sgwt_kernel_abspline3(double x);

	template<class T>
	void sgwt_cheby_coeff(int k, T g)		//n = m+1
	{
		int N = m + 1;
		double a1 = (arange[1] - arange[0]) / 2;
		double a2 = (arange[1] + arange[0]) / 2;
		//cout << c[k] << endl;
		//cout << c[k].size() << endl;
		for (int j = 0; j<m + 1; j++)
			for (int i = 1; i <= N; i++)
				c[k](j) += g(a1*cos((M_PI*(i - 0.5)) / N) + a2)*cos(M_PI*j*(i - 0.5) / N) * 2 / N;
	}

	VectorXd sgwt_setscales(const double lmin, const double lmax);

	vector<VectorXd> sgwt_cheby_op(VectorXd f, vector<VectorXd> c);

	void sgwt_filter_design(double lmax,Varargin varargin);

	void setArange(double lmin, double lmax);

	VectorXd sgwt_adjoint(vector<VectorXd> y);

	VectorXd sgwt_cheby_square(VectorXd c);
		
	VectorXd sgwt_inverse(vector<VectorXd> y);		//Known L, c, arange
													
	double sgwt_rough_lmax();

};