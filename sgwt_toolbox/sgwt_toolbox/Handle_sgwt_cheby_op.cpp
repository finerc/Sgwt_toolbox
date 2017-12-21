#include "Handle_sgwt_cheby_op.h"
#include "sgwt_toolbox.h"

Handle_sgwt_cheby_op::Handle_sgwt_cheby_op(int m, int Nscales, SpMat L, vector<VectorXd> c, double *arange)
{
	tmp_sgwt = new Sgwt(m, Nscales, L);
	tmp_sgwt->setArange(arange[0], arange[1]);
	d = c;
}

vector<VectorXd> Handle_sgwt_cheby_op::operator()(VectorXd x)
{
	return tmp_sgwt->sgwt_cheby_op(x, d);
}
