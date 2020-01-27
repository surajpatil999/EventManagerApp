/*Copyright (c) 2017 Developer Express Inc.
{*******************************************************************}
{                                                                   }
{       Developer Express Mobile Chart Library                      }
{                                                                   }
{                                                                   }
{       Copyright (c) 2017 Developer Express Inc.                   }
{       ALL RIGHTS RESERVED                                         }
{                                                                   }
{   The entire contents of this file is protected by U.S. and       }
{   International Copyright Laws. Unauthorized reproduction,        }
{   reverse-engineering, and distribution of all or any portion of  }
{   the code contained in this file is strictly prohibited and may  }
{   result in severe civil and criminal penalties and will be       }
{   prosecuted to the maximum extent possible under the law.        }
{                                                                   }
{   RESTRICTIONS                                                    }
{                                                                   }
{   THIS SOURCE CODE AND ALL RESULTING INTERMEDIATE FILES           }
{   ARE CONFIDENTIAL AND PROPRIETARY TRADE                          }
{   SECRETS OF DEVELOPER EXPRESS INC. THE REGISTERED DEVELOPER IS   }
{   LICENSED TO DISTRIBUTE THE PRODUCT AND ALL ACCOMPANYING         }
{   CONTROLS AS PART OF AN EXECUTABLE PROGRAM ONLY.                 }
{                                                                   }
{   THE SOURCE CODE CONTAINED WITHIN THIS FILE AND ALL RELATED      }
{   FILES OR ANY PORTION OF ITS CONTENTS SHALL AT NO TIME BE        }
{   COPIED, TRANSFERRED, SOLD, DISTRIBUTED, OR OTHERWISE MADE       }
{   AVAILABLE TO OTHER INDIVIDUALS WITHOUT EXPRESS WRITTEN CONSENT  }
{   AND PERMISSION FROM DEVELOPER EXPRESS INC.                      }
{                                                                   }
{   CONSULT THE END USER LICENSE AGREEMENT FOR INFORMATION ON       }
{   ADDITIONAL RESTRICTIONS.                                        }
{                                                                   }
{*******************************************************************}
*/
package com.itoneclick.buypassnow.dev_express;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;

import com.devexpress.dxcharts.Drawable;
import com.itoneclick.buypassnow.R;

public class LoadingDrawable implements Drawable {
    private Context context;
    private Paint textPaint;

    public LoadingDrawable(Context context) {
        this.context = context;
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize((int) context.getResources().getDimension(R.dimen.loading_text_size));
    }

    @Override
    public void onDraw(Canvas canvas) {
        textPaint.setColor(ContextCompat.getColor(context, R.color.color_loading_text));
        canvas.drawColor(ContextCompat.getColor(context, R.color.color_loading_background));
        canvas.drawText(context.getResources().getString(R.string.chart_loading),
                canvas.getWidth() / 2,
                canvas.getHeight() / 2 - ((textPaint.descent() + textPaint.ascent()) / 2),
                textPaint);
    }
}
