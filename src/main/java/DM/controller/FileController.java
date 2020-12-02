package DM.controller;


import DM.constant.FileTypeEnum;
import DM.entity.Channel_info;
import DM.entity.DbData;
import DM.entity.FileData;
import DM.entity.User_fault;
import DM.mapper.User_faultMapper;
import DM.util.FileTypeUtil;
import DM.util.ParseFile;
import com.mathworks.toolbox.javabuilder.MWException;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static DM.util.ParseFile.*;

/**
 * @description 文件服务器
 * @date 2020-11-24
 */
@Slf4j
@Controller
public class FileController {

    @Autowired
    private User_faultMapper userFaultMapper;

    private static final String SLASH = "/";

    private String fileDir;

    /**
     * 首页
     */
    @RequestMapping("/")
    public String index() {
        return "index.html";
    }

    /**
     * 上传文件
     *
     * @param file 文件
     * @param curPos 上传文件时所处的目录位置
     * @return Map
     */
    @ResponseBody
    @PostMapping("/file/upload")
    public Map upload(@RequestParam MultipartFile file, @RequestParam String curPos, User_fault user_fault) throws ParseException, ParseException {
        curPos = curPos.substring(1);
        if (fileDir == null) {
            fileDir = SLASH;
        }
        if (!fileDir.endsWith(SLASH)) {
            fileDir += SLASH;
        }
        // 文件原始名称
        String FileName = file.getOriginalFilename();
        int NameLen = FileName.length();
        String suffix = FileName.substring(FileName.lastIndexOf(".") + 1);
        String prefix = FileName.substring(0, FileName.lastIndexOf("."));
        String TestName = FileName.substring(0, NameLen-18);
        String strDateTime = FileName.substring(NameLen-18,NameLen-4);

        StringBuffer s = new StringBuffer();
        s.append(strDateTime.substring(0,4)+"-"+strDateTime.substring(4,6)+"-"+strDateTime.substring(6,8)+" "+strDateTime.substring(8,10)+":"+strDateTime.substring(10,12)+":"+strDateTime.substring(12,14));
        String temp = s.toString();

        //string转date类型
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date1 = null;
        date1 = sdf1.parse(temp);
        //date转datetime
        long longTime = date1.getTime();
        Timestamp timestamp = new Timestamp(longTime);

        //保存到磁盘
        String path;
        int index = 1;
        path = curPos + user_fault.getFaultName() + SLASH +"T"+user_fault.getTemperature()+"P"+user_fault.getPressure()+"F"+user_fault.getTraffic()+SLASH+user_fault.getTestType()+SLASH+ FileName;
        String true_path= fileDir+path;
        File outFile = new File(fileDir + path);
        while (outFile.exists()) {
            path = curPos+user_fault.getFaultName()+SLASH +"T"+user_fault.getTemperature()+"P"+user_fault.getPressure()+"F"+user_fault.getTraffic()+ SLASH +user_fault.getTestType()+SLASH+ FileName+ prefix + "(" + index + ")." + suffix;
            outFile = new File(fileDir + path);
            index++;
        }

        user_fault.setFileName(FileName);
        user_fault.setPath(true_path);
        user_fault.setFileTime(timestamp);
        userFaultMapper.addData(user_fault);
        
        try {
            if (!outFile.getParentFile().exists()) {
                outFile.getParentFile().mkdirs();
            }
            file.transferTo(outFile);
            Map rs = getRS(200, "上传成功", path );
            return rs;
        } catch (Exception e) {
            log.info(e.getMessage());
            return getRS(500, e.getMessage());
        }
    }
    /**
     * 搜索函数
    */
    @ResponseBody
    @PostMapping("/search")
    public DbData WriteExcel(@RequestParam String faultName,
                             @RequestParam String testType,
                             @RequestParam String member,
                             @RequestParam Integer pageNum,
                             @RequestParam Integer pageSize,
                             @RequestParam String degree,
                             @RequestParam String device,
                             @RequestParam String date) throws ParseException {

        Map<String, Object> map_query = new HashMap<>();
        int temp = 0;
        if(faultName.isEmpty() && testType.isEmpty() && member.isEmpty() && degree.isEmpty() && device.isEmpty() &&date.isEmpty())
            return new DbData();
        if(pageNum !=0)
            temp = (pageNum-1)*pageSize;

        if(!date.isEmpty()){
            String strtime1 = date.substring(0,19);
            String strtime2 = date.substring(22);
            //string转date类型
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date1 = null;
            date1 = sdf1.parse(strtime1);
            //date转datetime
            long longTime1 = date1.getTime();
            Timestamp timestamp1 = new Timestamp(longTime1);
            //string转date类型
            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date2 = null;
            date2 = sdf2.parse(strtime2);
            //date转datetime
            long longTime2 = date2.getTime();
            Timestamp timestamp2 = new Timestamp(longTime2);
            map_query.put("dateTime1",timestamp1);//时间加入Map中
            map_query.put("dateTime2",timestamp2);
        }
        map_query.put("faultName",faultName);
        map_query.put("testType",testType);
        map_query.put("degree",degree);
        map_query.put("device",device);
        map_query.put("pageNum",temp);
        map_query.put("pageSize",0);

        //easyExcel的导出数据到表格
        //EasyExcel.write(fileName, User_fault.class).sheet("test").doWrite(user_faults);

        List<User_fault> faults = userFaultMapper.queryData(map_query);
        int nums = faults.size();
        map_query.put("pageSize",pageSize);
        List<User_fault> faultPage = userFaultMapper.queryData(map_query);
        return new DbData(faultPage,nums);
    }

    /**
     * 获取源文件或者缩略图文件
     *
     * @param p
     * @param download 是否下载
     * @param response
     * @return
     */
   /* private String getFile(String p, boolean download, HttpServletResponse response) {

        if (fileDir == null) {
            fileDir = SLASH;
        }
        if (!fileDir.endsWith(SLASH)) {
            fileDir += SLASH;
        }
        outputFile(fileDir + p, download, response );
        return null;
    }*/

    /**
     * 查看/下载源文件
     *
     * @param p 文件全路径
     * @param d 是否下载,1-下载
     * @param response
     * @return
     */
    /*@GetMapping("/file")
    public String file(@RequestParam("p") String p,
                       @RequestParam(value = "d", required = true) int d,
                       HttpServletResponse response) {
        return getFile( p, d == 1 ? true : false, response );
    }*/


    /**
     * 查看缩略图
     *
     * @param p 文件全名
     * @param response
     * @return
     */
    /*@GetMapping("/file/sm")
    public String fileSm(@RequestParam("p") String p, HttpServletResponse response) {
        return getFile( p,false, response );
    }*/

    /**
     * 获取文件类型
     *
     * @param suffix
     * @param contentType
     * @return
     */
    private String getFileType(String suffix, String contentType) {
        String type;
        if (FileTypeEnum.PPT.getName().equalsIgnoreCase(suffix) || FileTypeEnum.PPTX.getName().equalsIgnoreCase(suffix)) {
            type = FileTypeEnum.PPT.getName();
        } else if (FileTypeEnum.DOC.getName().equalsIgnoreCase(suffix) || FileTypeEnum.DOCX.getName().equalsIgnoreCase(suffix)) {
            type = FileTypeEnum.DOC.getName();
        } else if (FileTypeEnum.XLS.getName().equalsIgnoreCase(suffix) || FileTypeEnum.XLSX.getName().equalsIgnoreCase(suffix)) {
            type = FileTypeEnum.XLS.getName();
        } else if (FileTypeEnum.PDF.getName().equalsIgnoreCase(suffix)) {
            type = FileTypeEnum.PDF.getName();
        } else if (FileTypeEnum.HTML.getName().equalsIgnoreCase(suffix) || FileTypeEnum.HTM.getName().equalsIgnoreCase(suffix)) {
            type = FileTypeEnum.HTM.getName();
        } else if (FileTypeEnum.TXT.getName().equalsIgnoreCase(suffix)) {
            type = FileTypeEnum.TXT.getName();
        } else if (FileTypeEnum.SWF.getName().equalsIgnoreCase(suffix)) {
            type = FileTypeEnum.FLASH.getName();
        } else if (FileTypeEnum.ZIP.getName().equalsIgnoreCase(suffix) || FileTypeEnum.RAR.getName().equalsIgnoreCase(suffix) || FileTypeEnum.SEVENZ.getName().equalsIgnoreCase(suffix)) {
            type = FileTypeEnum.ZIP.getName();
        } else if (contentType != null && contentType.startsWith(FileTypeEnum.AUDIO.getName() + SLASH)) {
            type = FileTypeEnum.MP3.getName();
        } else if (contentType != null && contentType.startsWith(FileTypeEnum.VIDEO.getName() + SLASH)) {
            type = FileTypeEnum.MP4.getName();
        } else {
            type = FileTypeEnum.FILE.getName();
        }
        return type;
    }

    /**
     * 获取全部文件
     *
     * @param dir
     * @param accept
     * @param exts
     * @return Map
     */
   // @Login
    @ResponseBody
    @RequestMapping("/api/list")
    public Map list(String dir, String accept, String exts) {
        String[] mExts = null;
        if (exts != null && !exts.trim().isEmpty()) {
            mExts = exts.split(",");
        }
        if (fileDir == null) {
            fileDir = SLASH;
        }
        if (!fileDir.endsWith(SLASH)) {
            fileDir += SLASH;
        }
        Map<String, Object> rs = new HashMap<>();
        if (dir == null || SLASH.equals(dir)) {
            dir = "";
        } else if (dir.startsWith(SLASH)) {
            dir = dir.substring(1);
        }
        File file = new File(fileDir + dir);
        File[] listFiles = file.listFiles();
        List<Map> dataList = new ArrayList<>();
        if (listFiles != null) {
            for (File f : listFiles) {
                if ("sm".equals(f.getName())) {
                    continue;
                }
                Map<String, Object> m = new HashMap<>(0);
                // 文件名称
                m.put( "name", f.getName() );
                // 修改时间
                m.put( "updateTime", f.lastModified() );
                // 是否是目录
                m.put( "isDir", f.isDirectory() );
                if (f.isDirectory()) {
                    // 文件类型
                    m.put( "type", "dir" );
                } else {
                    // 是否支持在线查看
                    boolean flag = false;
                    try {
                        if (FileTypeUtil.canOnlinePreview(new Tika().detect(f))) {
                            flag = true;
                        }
                        m.put( "preview", flag );
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // 文件地址
                    m.put( "url", (dir.isEmpty() ? dir : (dir + SLASH)) + f.getName() );
                    // 获取文件类型
                    String contentType = null;
                    String suffix = f.getName().substring( f.getName().lastIndexOf(".") + 1 );
                    try {
                        contentType = new Tika().detect(f);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // 筛选文件类型
                    if (accept != null && !accept.trim().isEmpty() && !accept.equals("file")) {
                        if (contentType == null || !contentType.startsWith( accept + SLASH )) {
                            continue;
                        }
                        if (mExts != null) {
                            for (String ext : mExts) {
                                if (!f.getName().endsWith( "." + ext )) {
                                    continue;
                                }
                            }
                        }
                    }
                    // 获取文件图标
                    m.put("type", getFileType(suffix, contentType));
                    // 是否有缩略图
                    String smUrl = "sm/" + (dir.isEmpty() ? dir : (dir + SLASH)) + f.getName();
                    if (new File(fileDir + smUrl ).exists()) {
                        m.put( "hasSm", true );
                        // 缩略图地址
                        m.put( "smUrl", smUrl );
                    }
                }
                dataList.add(m);
            }
        }
        // 根据上传时间排序
        Collections.sort(dataList, (o1, o2) -> {
            Long l1 = (long) o1.get("updateTime");
            Long l2 = (long) o2.get("updateTime");
            return l1.compareTo(l2);
        });
        // 把文件夹排在前面
        Collections.sort(dataList, (o1, o2) -> {
            Boolean l1 = (boolean) o1.get("isDir");
            Boolean l2 = (boolean) o2.get("isDir");
            return l2.compareTo(l1);
        });
        rs.put( "code", 200 );
        rs.put( "msg", "查询成功" );
        rs.put( "data", dataList );
        return rs;
    }

    /**
     * 递归删除目录下的文件以及目录
     *
     * @param file
     * @return
     */
    boolean forDelFile(File file) {
        if (!file.exists()) {
            return false;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                forDelFile(f);
            }
        }
        String s = file.toString();
        String replace = s.replace('\\', '/');
        userFaultMapper.delData(replace);

        return file.delete();
    }

    /**
     *显示根目录
     *
     */
   // @Login
    @ResponseBody
    @RequestMapping("/api/root")
    public boolean showRoot(String rootPath) {
        if(rootPath !=null){
            fileDir = rootPath;
            return true;
        }else
            return false;
    }

    /*
    *显示文件属性
    */
    @ResponseBody
    @RequestMapping("api/attribute")
    public User_fault showAttribute(String path){
        User_fault query = userFaultMapper.query(path);
        return query;
    }

    /*
     *显示此文件对应的时域频域波形
     */
    @ResponseBody
    @RequestMapping("/api/showfft")
    public FileData show_table(String path) throws IOException, MWException {
        if(path !=null){
            FileData fileData = ParseFile.parse(path);
            return fileData;
        }else
            return new FileData();
    }

    /*
     * 删除
     */
    @ResponseBody
    @RequestMapping("/api/del")
    public Map del(String file) {
        if (fileDir == null) {
            fileDir = SLASH;
        }
        if (!fileDir.endsWith(SLASH)) {
            fileDir += SLASH;
        }
        if (file != null && !file.isEmpty()) {
            File f = new File(fileDir + file );
            if (f.exists()) {
                // 文件
                if (f.isFile()) {
                    if (f.delete()) {
                        String pathf = fileDir+file;
                        userFaultMapper.delData(pathf);
                        return getRS(200, "文件删除成功");
                    }
                } else {
                    // 目录
                    forDelFile(f);
                    return getRS(200, "目录删除成功" );
                }
            } else {
                return getRS(500, "文件或目录不存在" );
            }
        }
        return getRS(500, "文件或目录删除失败" );
    }

    /**
     * 重命名文件夹
     *
     * @param oldFile
     * @param newFile
     * @return Map
     */
    @ResponseBody
    @RequestMapping("/api/rename")
    public Map rename(String oldFile, String newFile) {
        if (fileDir == null) {
            fileDir = SLASH;
        }
        if (!fileDir.endsWith(SLASH)) {
            fileDir += SLASH;
        }
        if (!StringUtils.isEmpty(oldFile) && !StringUtils.isEmpty(newFile)) {
            String oldpath = fileDir + oldFile;
            String newpath = fileDir + newFile;

            String newName = newFile.substring(newFile.lastIndexOf('/')+1);

            int count = 0;
            for (char achar : oldFile.toCharArray()) {
                if (achar == '/') count++;
            }

            File oldf = new File(fileDir + oldFile );
            File newf = new File(fileDir + newFile );

            if (oldf.renameTo(newf)) {

                Map<String, Object> mapUpdate = new HashMap<>();
                List<User_fault> us = userFaultMapper.queryFileOldPath(oldpath);

                if(count == 1){
                    //试验名称
                    mapUpdate.put("testName",newName);
                }else if(count == 2) {
                    //试验类型
                    mapUpdate.put("testType",newName);
                }

                for (User_fault fault : us) {
                    mapUpdate.put("oldpath", fault.getPath());
                    mapUpdate.put("newpath",fault.getPath().replace(oldpath,newpath));
                    userFaultMapper.upData(mapUpdate);
                }
                return getRS(200, "重命名成功", SLASH + newFile );
            }
        }
        return getRS(500, "重命名失败" );
    }

    /**
     * 封装返回结果
     *
     * @param code
     * @param msg
     * @param url
     * @return Map
     */
    private Map getRS(int code, String msg, String url) {
        Map<String, Object> map = new HashMap<>();
        map.put( "code", code );
        map.put( "msg", msg );
        if (url != null) {
            map.put( "url", url );
        }
        return map;
    }

    /**
     * 封装返回结果
     *
     * @param code
     * @param msg
     * @return Map
     */
    private Map getRS(int code, String msg) {
        return getRS(code, msg, null);
    }

    /**
     * 新建文件夹
     *
     * @param curPos
     * @param dirName
     * @return Map
     */
    @ResponseBody
    @RequestMapping("/api/mkdir")
    public Map mkdir(String curPos, String dirName) {
        if (fileDir == null) {
            fileDir = SLASH;
        }
        if (!fileDir.endsWith(SLASH)) {
            fileDir += SLASH;
        }
        if (!StringUtils.isEmpty(curPos) && !StringUtils.isEmpty(dirName)) {
            curPos = curPos.substring(1);
            String dirPath = fileDir + curPos + SLASH + dirName;
            File f = new File(dirPath);
            if (f.exists()) {
                return getRS( 500, "目录已存在" );
            }
            if (!f.exists() && f.mkdir()) {
                return getRS(200, "创建成功" );
            }
        }
        return getRS(500, "创建失败" );
    }

    /*
    * 返回区间内的数据
    */
    @ResponseBody
    @RequestMapping("/api/sectionData")
    public List<List<Float>> sectionData(String path, int left,int right,boolean flag_domain) throws IOException, MWException {
        File sourceFile = new File(path);
        if(!sourceFile.exists()) return new ArrayList<>();
        BufferedInputStream fis = new BufferedInputStream(new FileInputStream(sourceFile));
        int strLen = 0;
        //计算文件名字母个数，设置字节缓冲数组大小用来读取试验名称
        for (int i = 0, len = sourceFile.getName().length(); i < len; i++)
            if (Character.isDigit(sourceFile.getName().charAt(i))) {
                strLen = i;
                break;
            }
        byte[] buffer_str = new byte[strLen];// 缓冲区字节数组
        fis.read(buffer_4);//读取到字节数组的字节数
        int channel_num = ByteToInt(buffer_4);
        //通道信息
        List<Channel_info> channelLists = channelInfo(fis, channel_num, buffer_str);
        //时域数据信息
        List<List<Float>> domainLists = dataDomain(fis, channelLists);
        List<List<Float>> domainListRES = new ArrayList<>();

        for (int i = 0,len = domainLists.size(); i < len; i++)
            domainListRES.add(domainLists.get(i).subList(left,right));
        //判断时域频域标志位来决定返回的数据
        if(flag_domain)
            return domainListRES;
        else {
            List<List<Float>> frequencyRES = fft_pure(domainListRES, false);
            return frequencyRES;
        }
    }
}
