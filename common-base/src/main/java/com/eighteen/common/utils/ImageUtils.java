package com.eighteen.common.utils;

import org.apache.commons.lang3.math.NumberUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Created by wangwei.
 * Date: 2020/3/2
 * Time: 22:06
 */
public class ImageUtils {
    public static File graphicsGeneration(List<List<List<String>>> allValue, List<String> titles, List<String[]> headers, String receiver, int totalcol) throws Exception {
        int rows = 0;
        for (List<List<String>> typeV : allValue) {
            if (typeV != null && typeV.size() > 0) {
                rows += (2 + typeV.size());
            }
        }
        // 实际数据行数+标题+备注
        int totalrow = 1 + rows;
        int imageWidth = 1000;
        int imageHeight = totalrow * 30 + 20;
        int rowheight = 30;
        int startHeight = 10;
        int startWidth = 10;
        int colwidth = ((imageWidth - 20) / totalcol);

        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.getGraphics();

        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, imageWidth, imageHeight);
        //画背景
        graphics.setColor(new Color(0, 112, 192));
        int startH = 1;
        for (List<List<String>> typeV : allValue) {
            if (typeV != null && typeV.size() > 0) {
                graphics.fillRect(startWidth + 1, startHeight + startH * rowheight + 1, imageWidth - startWidth - 5 - 1, rowheight - 1);
                startH += 2 + typeV.size();
            }
        }

        graphics.setColor(new Color(220, 240, 240));
        Font font = new Font("宋体", Font.BOLD, 20);
        graphics.setFont(font);

        // 画横线

        for (int j = 0; j < totalrow - 1; j++) {
            graphics.setColor(Color.black);
            graphics.drawLine(startWidth, startHeight + (j + 1) * rowheight, imageWidth - 5,
                    startHeight + (j + 1) * rowheight);
        }

        // 画竖线
        graphics.setColor(Color.black);

        startH = 1;
        int rightLine = 0;
        for (List<List<String>> typeV : allValue) {

            if (typeV != null && typeV.size() > 0) {
                for (int k = 0; k < totalcol + 1; k++) {
                    rightLine = getRightMargin(k, startWidth, colwidth, imageWidth);
                    graphics.drawLine(rightLine, startHeight + startH * rowheight, rightLine,
                            startHeight + (typeV.size() + 1 + startH) * rowheight);
                }
                startH += 2 + typeV.size();
            }
        }

        // 设置字体
        graphics.setFont(font);

        // 写标题
        startH = 1;
        int i = 0;
        for (List<List<String>> typeV : allValue) {
            if (typeV != null && typeV.size() > 0) {
                graphics.drawString(titles.get(i), imageWidth / 3 + startWidth + 30, startHeight + startH * rowheight - 10);
                startH += 2 + typeV.size();
            }
            i++;
        }


        // 写入表头
        graphics.setColor(Color.WHITE);
        font = new Font("宋体", Font.BOLD, 20);
        graphics.setFont(font);
        startH = 2;
        i = 0;
        for (List<List<String>> typeV : allValue) {
            if (typeV != null && typeV.size() > 0) {

                String[] headCells = headers.get(i);
                for (int m = 0; m < headCells.length; m++) {
                    rightLine = getRightMargin(m, startWidth, colwidth, imageWidth) + 5;
                    graphics.drawString(headCells[m], rightLine,
                            startHeight + rowheight * startH - 10);
                }
                startH += 2 + typeV.size();
            }
            i++;
        }


        // 写入内容
        graphics.setColor(Color.red);
        font = new Font("宋体", Font.PLAIN, 20);
        graphics.setFont(font);
        startH = 3;
        i = 0;
        for (List<List<String>> typeV : allValue) {
            if (typeV != null && typeV.size() > 0) {
                for (int n = 0; n < typeV.size(); n++) {
                    List<String> arr = typeV.get(n);
                    for (int l = 0; l < arr.size(); l++) {
                        rightLine = getRightMargin(l, startWidth, colwidth, imageWidth) + 5;
                        String str = arr.get(l);
                        String substring = str.substring(0, str.length() - 1);
                        if (str.endsWith("%") && NumberUtils.isCreatable(substring)) {
                            if (str.startsWith("-"))
                                graphics.setColor(Color.green);
                            else {
                                graphics.setColor(Color.red);
                            }
                        } else
                            graphics.setColor(Color.black);
                        graphics.drawString(str, rightLine,
                                startHeight + rowheight * (n + startH) - 10);
                    }
                }
                startH += 2 + typeV.size();
            }
            i++;
        }

        String path = "1.jpg";
        File output = new File(path);
        ImageIO.write(image, "jpg", output);
        return output;
    }

    /**
     * 获取竖线和文字的水平位置
     *
     * @param k
     * @param startWidth
     * @param colwidth
     * @param imageWidth
     * @return
     */
    private static int getRightMargin(int k, int startWidth, int colwidth, int imageWidth) {
        int rightLine = 0;
        if (k == 0) {
            rightLine = startWidth;
        } else if (k == 1) {
            rightLine = startWidth + colwidth / 2;
        } else if (k == 2) {
            rightLine = startWidth + 3 * colwidth / 2;
        } else if (k == 3) {
            rightLine = startWidth + 5 * colwidth / 2;
        } else if (k == 4) {
            rightLine = imageWidth - 5;
        }
        return rightLine;
    }

}
