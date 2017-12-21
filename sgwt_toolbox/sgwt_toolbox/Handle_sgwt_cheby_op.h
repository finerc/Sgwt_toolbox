#pragma once
#include <Eigen/Dense>
#include "vector"
#include <Eigen/Sparse>
using namespace std;
using namespace Eigen;

typedef Eigen::SparseMatrix<double> SpMat;

class Sgwt;

class Handle_sgwt_cheby_op {
private:
	Sgwt *tmp_sgwt;
	vector<VectorXd> d;
public:
	Handle_sgwt_cheby_op(int m,int Nscales, SpMat L, vector<VectorXd> c, double *arange);

	vector<VectorXd> operator()(VectorXd x);
};
