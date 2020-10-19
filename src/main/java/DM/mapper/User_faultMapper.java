package DM.mapper;

import DM.entity.User_fault;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface User_faultMapper {

    //模糊查询
    List<User_fault> queryData(Map map);
    //添加数据
    void addData(User_fault user_fault);
    //删除数据
    void delData(String path);
    //查询单条数据
    User_fault query(String path);
    //查询修改文件夹下的文件
    List<User_fault> queryFileOldPath(String oldpath);
    //修改名称
    void upData(Map map);
}
