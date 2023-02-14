$(function(){
    $("#code-check").click(send);
    $("#your-email").focus(clear_error);
});
function send(){
    //发送AJAX请求之前，将CSRF令牌设置到请求消息头中
	var token=$("meta[name='_csrf']").attr("content");
	var header=$("meta[name='_csrf_header']").attr("content");
    $(document).ajaxSend(function(e,xhr,options){
        xhr.setRequestHeader(header,token)
    });
    var email=$("#your-email").val();
    $.post(
        CONTEXT_PATH+"/user/forget/send",
        {"email":email},
        function(data){
            data=$.parseJSON(data);
            if(data.code==0){
                $("#code-check").text("已发送,有效时间60s");
            }else{
                alert(data.msg);
            }
        }
    );
}
function clear_error(){
    $("#code-check").text("获取验证码");
}