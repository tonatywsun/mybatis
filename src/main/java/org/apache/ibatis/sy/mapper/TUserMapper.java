package org.apache.ibatis.sy.mapper;

import org.apache.ibatis.sy.model.TUser;

/**
 * @Description: TODO
 * @Author: tona.sun
 * @Date: 2020/03/02 16:06
 */
public interface TUserMapper {
  TUser selectByPrimaryKey(Integer id);
}
