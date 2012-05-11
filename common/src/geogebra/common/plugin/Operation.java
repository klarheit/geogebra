package geogebra.common.plugin;

public enum Operation { NO_OPERATION, NOT_EQUAL, NOT, OR,AND,IMPLICATION,EQUAL_BOOLEAN,LESS,GREATER,LESS_EQUAL
	,GREATER_EQUAL,PARALLEL,PERPENDICULAR ,IS_ELEMENT_OF,IS_SUBSET_OF
	,IS_SUBSET_OF_STRICT,SET_DIFFERENCE,PLUS,MINUS,VECTORPRODUCT,

// these next three must be adjacent
// so that brackets work for eg a/(b/c)
// and are removed in (a/b)/c
// see case DIVIDE in ExpressionNode
MULTIPLY,MULTIPLY_OR_FUNCTION,DIVIDE,POWER,

FREEHAND,COS,SIN,TAN,EXP,LOG,ARCCOS,ARCSIN,ARCTAN,ARCTAN2,SQRT,ABS
,SGN,XCOORD,YCOORD,ZCOORD,COSH,SINH,TANH,ACOSH,ASINH,ATANH,CSC,SEC
,COT,CSCH,SECH,COTH,FLOOR,CEIL,FACTORIAL,ROUND,GAMMA,GAMMA_INCOMPLETE
,GAMMA_INCOMPLETE_REGULARIZED,BETA,BETA_INCOMPLETE,BETA_INCOMPLETE_REGULARIZED
,ERF,PSI,POLYGAMMA,LOG10,LOG2,LOGB,CI,SI,EI,CBRT,RANDOM,CONJUGATE,ARG,FUNCTION,FUNCTION_NVAR,
VEC_FUNCTION,DERIVATIVE,ELEMENT_OF,  

// spreadsheet absolute reference using $ signs
$VAR_ROW,$VAR_COL,$VAR_ROW_COL,

ARBCONST,ARBINT,ARBCOMPLEX}