#include "sgwt_toolbox.h"
#include "fminbnd.h"
#include "pcg.h"
#include "time.h"

double G1(double x)
{
	return exp(-pow(x,4));
}

double G1P(double x)
{
	return -4 * pow(x, 3)*exp(-pow(x, 4));
}

Sgwt::Sgwt(const int _m, const int _Nscales, SpMat _L) :m(_m), Nscales(_Nscales),L(_L)
{
	arange[0] = -1;
	arange[1] = 1;
	g = new GN[_Nscales];	
	VectorXd init_c(m + 1);
	init_c.setZero();
	for (int i = 0; i < _Nscales + 1; i++)
		c.push_back(init_c);
};

void Sgwt::setArange(const double lmin, const double lmax)
{
	arange[0] = lmin;
	arange[1] = lmax;
}

double Sgwt::sgwt_kernel_abspline3(double x)
{
	int alpha = 2, beta = 2, t1 = 1, t2 = 2;
	Matrix4d M;
	M << 1, t1, t1*t1, t1*t1*t1,
		1, t2, t2*t2, t2*t2*t2,
		0, 1, 2 * t1, 3 * t1*t1,
		0, 1, 2 * t2, 3 * t2*t2;
	Vector4d v;
	v << 1, 1, pow(t1, -alpha)*alpha*pow(t1, alpha - 1), -beta*pow(t2, -beta - 1)*pow(t2, beta);
	Vector4d a;
	a = M.inverse()*v;
	if (x >= 0 && x < t1)
		return pow(x, alpha)*pow(t1, -alpha);
	else if (x >= t1&&x < t2)
		return a(0) + a(1)*x + a(2)*x*x + a(3)*x*x*x;
	else
		return pow(x, -beta)*pow(t2, beta);
}

double Sgwt::_sgwt_kernel_abspline3(double x)
{
	return -sgwt_kernel_abspline3(x);
}
	

vector<VectorXd> Sgwt::sgwt_cheby_op(VectorXd f,vector<VectorXd> c)
{
	clock_t start, finish;
	double duration;
	start = clock();
	int cur_nscales = c.size();
	VectorXd M(cur_nscales);
	vector<VectorXd> r;
	VectorXd init_r(f.size());
	init_r.setZero();
	for (int i = 0; i < cur_nscales; i++)
		r.push_back(init_r);
	int maxM = 0;
	for (int i = 0; i < cur_nscales; i++)
	{
		M[i] = c[i].size();	
		if (M[i] > maxM)
			maxM = M[i];
	}

	double a1 = (arange[1] - arange[0]) / 2;
	double a2 = (arange[1] + arange[0]) / 2;

	VectorXd Twf_old(f);
	VectorXd Twf_cur(f.size());
	Twf_cur = (L*f - a2*f) / a1;
	for (int i = 0; i < cur_nscales; i++)
		r[i] = 0.5*c[i](0)*Twf_old + c[i](1)*Twf_cur;

	VectorXd Twf_new;
	for (int k = 1; k < maxM; k++)
	{
		/*VectorXd tmp = L*Twf_cur;		//< 3ms
		Twf_new = (tmp - a2*Twf_cur);		// <2ms	
		Twf_new *= 2 / a1;					// <1ms
		Twf_new-= Twf_old;				// <1ms*/
		Twf_new = (2.0 / a1)*(L*Twf_cur - a2*Twf_cur) - Twf_old;			// 6~7ms
		for (int j = 0; j < cur_nscales; j++)
		{
			if (1 + k < M(j))
				r[j] = r[j] + c[j](k+1)*Twf_new;
		}
		Twf_old = Twf_cur;
		Twf_cur = Twf_new;
	}
	finish = clock();
	duration = (double)(finish - start) / CLOCKS_PER_SEC;
	return r;
}

VectorXd Sgwt::sgwt_setscales(double lmin, double lmax)
{
	int t1 = 1;
	int t2 = 2;

	double smin = t1 / lmax;
	double smax = t2 / lmin;
	//scales should be decreasing ... higher j should give larger s
	VectorXd s = VectorXd::LinSpaced(Nscales, log(smax), log(smin));				//VectorXd::LinSpaced(size,low,high)
	for (int i = 0; i < Nscales; i++)
		s[i] = exp(s[i]);
	return s;
}

void Sgwt::sgwt_filter_design(double lmax, Varargin varargin)
{
	if (varargin.designtype == "abspline3")					//暂时先实现使用的这一种
	{
		double lmin = lmax / varargin.lpfactor;
		t = sgwt_setscales(lmin, lmax);
		G g1 = G1;
		G g1p = G1P;
		//G gb = sgwt_kernel_abspline3;
		G f = _sgwt_kernel_abspline3;

		for (int j = 0; j < Nscales; j++) {
			g[j].setTj(t[j]);
		}
		/*	% find maximum of g's ...
			% I could get this analytically as it is a cubic spline, but
			% this also works.*/
		double xstar = fminbnd(f, 1, 2);
		//cout << xstar << endl;   !!!
		double gamma_l = -(f(xstar));
		//cout << gamma_l << endl; !!!
		double lminfac = .6*lmin;
		g0 = new G0(g1, lminfac,gamma_l);
	}
}

VectorXd Sgwt::sgwt_adjoint(vector<VectorXd> y)
{
	VectorXd adj(L.rows());
	adj.setZero();

	for (int j = 0; j < c.size(); j++)
	{
		vector<VectorXd> _c;
		_c.push_back(c[j]);
		VectorXd tmp = sgwt_cheby_op(y[j], _c)[0];
		adj = adj + tmp;
	}
	return adj;
}

VectorXd Sgwt::sgwt_cheby_square(VectorXd c)
{
	int M = c.size() - 1;
	VectorXd cp(c);
	cp(0) = 0.5*c(0);

	VectorXd dp(2 * M + 1);
	dp.setZero();

	for (int m = 0; m <= 2 * M; m++)
	{
		if (m == 0)
		{
			dp(m) = dp(m) + 0.5*cp(0)*cp(0);
			for (int i = 0; i <= M; i++)
				dp(m) = dp(m) + 0.5*cp(i)*cp(i);
		}
		else if (m <= M)
		{
			for (int i = 0; i <= m; i++)
				dp(m) = dp(m) + 0.5*cp(i)*cp(m - i);
			for (int i = 0; i <= M - m; i++)
				dp(m) = dp(m) + 0.5*cp(i)*cp(m + i);
			for (int i = m; i <= M; i++)
				dp(m) = dp(m) + 0.5*cp(i)*cp(i - m);
		}
		else
		{
			for (int i = m - M; i <= M; i++)
			{
				dp(m) = dp(m) + 0.5*cp(i)*cp(m - i);
			}
		}
	}
	VectorXd d(dp);
	d(0) = 2 * dp(0);
	return d;
}

VectorXd Sgwt::sgwt_inverse(vector<VectorXd> y)
{
	double tol = 1e-6;
	int N = L.rows();
	clock_t start, finish;
	double duration;
	start = clock();
	VectorXd adj = sgwt_adjoint(y);
	finish = clock();
	duration = (double)(finish - start) / CLOCKS_PER_SEC;
	cout << "time for sgwt_adjoint is " << duration << " seconds" << endl;

	int maxM = 0;
	VectorXi M(c.size());
	for (int i = 0; i < c.size(); i++)
	{
		M[i] = c[i].size();
		if (M[i] > maxM)
			maxM = M[i];
	}

	VectorXd init_d(1 + 2 * (maxM - 1));
	init_d.setZero();
	vector<VectorXd> d;
	d.push_back(init_d);

	start = clock();
	vector<VectorXd> cpad;
	for (int j = 0; j < c.size(); j++)
	{
		VectorXd init_cpad(maxM);
		for (int i = 0; i < M[j]; i++)
			init_cpad[i] = c[j](i);
		cpad.push_back(init_cpad);
		VectorXd S = sgwt_cheby_square(cpad[j]);
		d[0] = d[0] + S;
	}
	finish = clock();
	duration = (double)(finish - start) / CLOCKS_PER_SEC;
	cout << "time for sgwt_cheby_square is " << duration << " seconds" << endl;

	start = clock();
	Handle_sgwt_cheby_op wstarw(m, Nscales, L, d, arange);
	VectorXd r = pcg(wstarw, adj, tol);
	finish = clock();
	duration = (double)(finish - start) / CLOCKS_PER_SEC;
	cout << "time for PCG is " << duration << " seconds" << endl;
	return r; 
}

double Sgwt::sgwt_rough_lmax()
{
	SparseGenMatProd<double> op(L);
	// Construct eigen solver object, requesting the largest three eigenvalues
	GenEigsSolver< double, LARGEST_MAGN, SparseGenMatProd<double> > eigs(&op, 1, 10);

	// Initialize and compute
	eigs.init();
	int nconv = eigs.compute();

	
	// Retrieve results
	Eigen::VectorXcd evalues;
	if (eigs.info() == SUCCESSFUL)
	{
		evalues = eigs.eigenvalues();
	}
	
	std::cout << "Eigenvalues found:\n" << evalues(0).real()*1.01 << std::endl;
	return evalues(0).real()*1.01;
}
