$(function(){
    $("#uploadPassword").submit(check_data);
    $("#confirm-password").focus(clear_error);
    $("#uploadForm").submit(upload);
});
function upload(){
    //发送AJAX请求之前，将CSRF令牌设置到请求消息头中
	var token=$("meta[name='_csrf']").attr("content");
	var header=$("meta[name='_csrf_header']").attr("content");
    $(document).ajaxSend(function(e,xhr,options){
        xhr.setRequestHeader(header,token)
    });
    $.ajax({
        url:"http://upload-z2.qiniup.com",
        method:"post",
        processData:false,
        contentType:false,
        data:new FormData($("#uploadForm")[0]),
        success:function(data){
            if(data && data.code==0){

                //更新头像访问路径
                $.post(
                    CONTEXT_PATH+"/user/header/url",
                    {"fileName":$("input[name='key']").val()},
                    function(data){
                        data = $.parseJSON(data);
                        if(data.code==0){
                            location.href=CONTEXT_PATH+"/index";
                        }else{
                            alert(data.msg);
                        }
                    }
                );
             }else{
                alert("上传失败！");
             }

        }
    });
    return false;
}
function check_data() {
	var pwd1 = $("#new-password").val();
	var pwd2 = $("#confirm-password").val();
	if(pwd1 != pwd2) {
		$("#confirm-password").addClass("is-invalid");
		return false;
	}
	return true;
}

function clear_error() {
	$("#confirm-password").removeClass("is-invalid");
}