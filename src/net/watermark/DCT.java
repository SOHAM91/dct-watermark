package net.watermark;

// 程式名稱：DCT.java
// 程式功能：DCT 類別,含ForwardDCT與InverseDCT兩種方法class DCT
// 執行範例：java embed

public class DCT {
    private final int n;

    public double c[][] = new double[this.n][this.n];

    public double ct[][] = new double[this.n][this.n];

    public DCT() {
        this(8);
    }

    public DCT(final int N) {
        this.n = N;
        int i;
        int j;
        final double pi = Math.atan(1.0) * 4.0;
        for (j = 0; j < N; j++) {
            this.c[0][j] = 1.0 / Math.sqrt(N);
            this.ct[j][0] = this.c[0][j];
        }
        for (i = 1; i < N; i++) {
            for (j = 0; j < N; j++) {
                this.c[i][j] = Math.sqrt(2.0 / N) * Math.cos(pi * (2 * j + 1) * i / (2.0 * N));
                this.ct[j][i] = this.c[i][j];
            }
        }
    }

    void ForwardDCT(final int input[][], final int output[][]) {
        final double temp[][] = new double[this.n][this.n];
        double temp1;
        int i, j, k;
        for (i = 0; i < this.n; i++) {
            for (j = 0; j < this.n; j++) {
                temp[i][j] = 0.0;
                for (k = 0; k < this.n; k++) {
                    temp[i][j] += (input[i][k] - 128) * this.ct[k][j];
                }
            }
        }

        for (i = 0; i < this.n; i++) {
            for (j = 0; j < this.n; j++) {
                temp1 = 0.0;
                for (k = 0; k < this.n; k++) {
                    temp1 += this.c[i][k] * temp[k][j];
                }
                output[i][j] = (int) Math.round(temp1);
            }
        }
    }

    void InverseDCT(final int input[][], final int output[][]) {
        final double temp[][] = new double[this.n][this.n];
        double temp1;
        int i, j, k;

        for (i = 0; i < this.n; i++) {
            for (j = 0; j < this.n; j++) {
                temp[i][j] = 0.0;
                for (k = 0; k < this.n; k++) {
                    temp[i][j] += input[i][k] * this.c[k][j];
                }
            }
        }

        for (i = 0; i < this.n; i++) {
            for (j = 0; j < this.n; j++) {
                temp1 = 0.0;
                for (k = 0; k < this.n; k++) {
                    temp1 += this.ct[i][k] * temp[k][j];
                }
                temp1 += 128.0;
                if (temp1 < 0) {
                    output[i][j] = 0;
                } else if (temp1 > 255) {
                    output[i][j] = 255;
                } else {
                    output[i][j] = (int) Math.round(temp1);
                }
            }
        }
    }
}
