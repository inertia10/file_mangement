package DM.controller;

import DM.annotation.Login;
import DM.constant.FileTypeEnum;
import DM.entity.DbData;
import DM.entity.FileData;
import DM.entity.User;
import DM.entity.User_fault;
import DM.mapper.User_faultMapper;
import DM.util.FileTypeUtil;
import DM.util.ParseFile;
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
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @description 文件服务器
 * @date 2019-1-21
 */
@Slf4j
//@CrossOrigin
@Controller
public class FileController {

    @Autowired
    private User_faultMapper userFaultMapper;

    private static final String SLASH = "/";

    private String fileDir;

    @Value("${fs.useSm}")
    private Boolean useSm;

    @Value("${admin.uname}")
    private String uname;

    @Value("${admin.pwd}")
    private String pwd;

    /**
     * 首页
     *
     * @return
     */
    @Login
    @RequestMapping("/")
    public String index() {
        return "index.html";
    }
    /**
     * 登录页
     *
     * @return
     */
    @RequestMapping("/login")
    public String loginPage() {
        return "login.html";
    }

    /**
     * 登录提交认证
     *
     * @param user
     * @param session
     * @return
     */
    @PostMapping("/auth")
    public String auth(User user, HttpSession session) {
        if (user.getUname().equals(uname) && user.getPwd().equals(pwd)) {
            session.setAttribute( "LOGIN_USER", user );
            return "redirect:/";
        }
        return "redirect:/login";
    }


    /**
     * 上传文件
     *
     * @param file 文件
     * @param curPos 上传文件时所处的目录位置
     * @return Map
     */
    @Login
    @ResponseBody
    @PostMapping("/file/upload")
    public Map upload(@RequestParam MultipartFile file, @RequestParam String curPos, User_fault user_fault) {
        curPos = curPos.substring(1) + SLASH;
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
        String TestSpecificTime = FileName.substring(NameLen-18,NameLen-4);
        String TestDate = FileName.substring(NameLen-18,NameLen-10);
        String TestTime = FileName.substring(NameLen-10,NameLen-4);
        // 保存到磁盘
        File outFile;
        String path;
        int index = 1;
        path = curPos + user_fault.getFaultName() + SLASH +user_fault.getTestType()+SLASH+ FileName;
        String true_path= fileDir+path;
        outFile = new File(fileDir + path);
        while (outFile.exists()) {
            path = curPos + user_fault.getFaultName() + SLASH +user_fault.getTestType()+SLASH+ FileName+ prefix + "(" + index + ")." + suffix;
            outFile = new File(fileDir + path);
            index++;
        }

        user_fault.setFileName(FileName);
        user_fault.setPath(true_path);
        user_fault.setTime(TestSpecificTime);
        userFaultMapper.addData(user_fault);
        
        try {
            if (!outFile.getParentFile().exists()) {
                outFile.getParentFile().mkdirs();
            }
            file.transferTo(outFile);
            Map rs = getRS(200, "上传成功", path );
            //生成缩略图
            if (useSm != null && useSm) {
                // 获取文件类型
                String contentType = null;
                try {
                    contentType = new Tika().detect(outFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (contentType != null && contentType.startsWith( "image/" )) {
                    File smImg = new File(fileDir + "sm/" + path );
                    if (!smImg.getParentFile().exists()) {
                        smImg.getParentFile().mkdirs();
                    }
                    Thumbnails.of(outFile).scale(1f).outputQuality(0.25f).toFile(smImg);
                    rs.put( "smUrl", "sm/" + path );
                }
            }
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
    public DbData WriteExcel(@RequestParam String faultName, @RequestParam String testType, @RequestParam String member, @RequestParam Integer pageNum, @RequestParam Integer pageSize,@RequestParam String degree,@RequestParam String device){
        //String fileName = "C:\\Users\\Wang.Cao\\Desktop\\FaultTest.xlsx";
        Map<String, Object> map_query = new HashMap<>();
        int temp = 0;
        if(faultName.isEmpty() && testType.isEmpty() && member.isEmpty() && degree.isEmpty() && device.isEmpty())
            return new DbData();
        if(pageNum !=0)
            temp = (pageNum-1)*pageSize;
        map_query.put("faultName",faultName);
        map_query.put("testType",testType);
        map_query.put("degree",degree);
        map_query.put("device",device);
        map_query.put("pageNum",temp);
        map_query.put("pageSize",0);
//        map_query.put("time1",0);
//        map_query.put("pageSiz",0);

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
    private String getFile(String p, boolean download, HttpServletResponse response) {

        if (fileDir == null) {
            fileDir = SLASH;
        }
        if (!fileDir.endsWith(SLASH)) {
            fileDir += SLASH;
        }
        outputFile(fileDir + p, download, response );
        return null;
    }

    /**
     * 查看/下载源文件
     *
     * @param p 文件全路径
     * @param d 是否下载,1-下载
     * @param response
     * @return
     */
    @Login
    @GetMapping("/file")
    public String file(@RequestParam("p") String p,
                       @RequestParam(value = "d", required = true) int d,
                       HttpServletResponse response) {
        return getFile( p, d == 1 ? true : false, response );
    }


    /**
     * 查看缩略图
     *
     * @param p 文件全名
     * @param response
     * @return
     */
    @Login
    @GetMapping("/file/sm")
    public String fileSm(@RequestParam("p") String p, HttpServletResponse response) {
        return getFile( p,false, response );
    }

    /**
     * 输出文件流
     *
     * @param file
     * @param download 是否下载
     * @param response
     */
    private void outputFile(String file, boolean download, HttpServletResponse response) {
        // 判断文件是否存在
        File inFile = new File(file);
        // 文件不存在
        if (!inFile.exists()) {
            PrintWriter writer = null;
            try {
                response.setContentType("text/html;charset=UTF-8");
                writer = response.getWriter();
                writer.write("<!doctype html><title>404 Not Found</title><link rel=\"shorcut icon\" href=\"assets/images/logo.png\"><h1 style=\"text-align: center\">404 Not Found</h1><hr/><p style=\"text-align: center\">FMS Server</p>");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        // 获取文件类型
        String contentType = null;
        try {
            contentType = new Tika().detect(inFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 图片、文本文件,则在线查看
        //log.info("文件类型：" + contentType);
        if (FileTypeUtil.canOnlinePreview(contentType) && !download) {
            response.setContentType(contentType);
            response.setCharacterEncoding("UTF-8");
        } else {
            // 其他文件,强制下载
            response.setContentType( "application/force-download" );
            String newName;
            try {
                newName = URLEncoder.encode( inFile.getName(), "utf-8" );
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                newName = inFile.getName();
            }
            response.setHeader("Content-Disposition", "attachment;fileName=" + newName );
        }
        // 输出文件流
        OutputStream os = null;
        FileInputStream is = null;
        try {
            is = new FileInputStream(inFile);
            os = response.getOutputStream();
            byte[] bytes = new byte[1024];
            int len;
            while ((len = is.read(bytes)) != -1) {
                os.write(bytes, 0, len);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

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
    @Login
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
                    String type;
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
    static boolean forDelFile(File file) {
        if (!file.exists()) {
            return false;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                forDelFile(f);
            }
        }
        return file.delete();
    }

    /**
     *显示根目录
     *
     */
    @Login
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
    *
    */
    @Login
    @ResponseBody
    @RequestMapping("api/attribute")
    public User_fault showAttribute(String path){
        User_fault query = userFaultMapper.query(path);
        return query;
    }
    /**
     *显示此文件对应的时域频域波形
     *
     */
    @Login
    @ResponseBody
    @RequestMapping("/api/show")
    public FileData show_table(String path) throws IOException {
        if(path !=null){
            FileData fileData = ParseFile.parse(path);
            return fileData;
        }else {
            return new FileData();
        }
    }

    /**
     * 删除
     *
     * @param file
     * @return Map
     */
    @Login
    @ResponseBody
    @RequestMapping("/api/del")
    public Map del(String file,String path) {
        if (fileDir == null) {
            fileDir = SLASH;
        }
        if (!fileDir.endsWith(SLASH)) {
            fileDir += SLASH;
        }
        if (path !=null) {
            File fi = new File(path);
            if(fi.exists())
                fi.delete();
            userFaultMapper.delData(path);
            return getRS(200, "文件删除成功");
        }
        if (file != null && !file.isEmpty()) {
            File f = new File(fileDir + file );
            //File smF = new File(fileDir + "sm/" + file );
            if (f.exists()) {
                // 文件
                if (f.isFile()) {
                    if (f.delete()) {
//                        if (smF.exists() && smF.isFile()) {
//                            smF.delete();
//                        }
                        String pathf = fileDir+file;
                        System.out.println(pathf);
                        userFaultMapper.delData(pathf);
                        return getRS(200, "文件删除成功");
                    }
                } else {
                    // 目录
                    forDelFile(f);
//                    if (smF.exists() && smF.isDirectory()) {
//                        forDelFile(smF);
//                    }
                    return getRS(200, "目录删除成功" );
                }
            } else {
                return getRS(500, "文件或目录不存在" );
            }
        }
        return getRS(500, "文件或目录删除失败" );
    }

    /**
     * 重命名
     *
     * @param oldFile
     * @param newFile
     * @return Map
     */
    @Login
    @ResponseBody
    @RequestMapping("/api/rename")
    public Map rename(String oldFile, String newFile,User_fault user_fault) {
        if (fileDir == null) {
            fileDir = SLASH;
        }
        if (!fileDir.endsWith(SLASH)) {
            fileDir += SLASH;
        }
        if (!StringUtils.isEmpty(oldFile) && !StringUtils.isEmpty(newFile)) {
            File f = new File(fileDir + oldFile );
            File smF = new File(fileDir + "sm/" + oldFile );
            File nFile = new File(fileDir + newFile );
            File nsmFile = new File(fileDir + "sm/" + newFile );

            if (f.renameTo(nFile)) {
                if (smF.exists()) {
                    smF.renameTo(nsmFile);
                }
                String fileName = newFile.substring(newFile.lastIndexOf('/')+1);
                String path1 = nFile.getPath();
                String path = path1.replaceAll("\\\\","/");
                int numLength = fileName.length();

                String Time = fileName.substring(numLength-18,numLength-4);
                user_fault.setFileName(fileName);
                user_fault.setPath(path);
                user_fault.setTime(Time);

                userFaultMapper.upData(user_fault);
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
    @Login
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
}
