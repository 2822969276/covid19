package cn.itcast.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用来封装防疫物资的javaBean
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MateriaBean {
    private String name;//物资名称
    private String from;//物资来源
    private Integer count;//物资数量

}
