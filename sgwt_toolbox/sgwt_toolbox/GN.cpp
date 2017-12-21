#include "sgwt_toolbox.h"
#include "GN.h"

double GN::operator()(double x)
{
	x = x*tj;
	return Sgwt::sgwt_kernel_abspline3(x);
}

GN::GN()
{
	tj = 0;
}

void GN::setTj(const double tj)
{
	this->tj = tj;
}