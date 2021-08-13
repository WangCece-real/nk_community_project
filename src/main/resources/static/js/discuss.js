function like(btn, entityType, entityId, entityUserId, postId) {
    $.post(
        // 访问路径
        CONTEXT_PATH + "/like",
        //
        {"entityType":entityType,"entityId":entityId,"entityUserId":entityUserId, "postId":postId},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {
                // 改变赞的数量和赞的字
                $(btn).children("i").text(data.likeCount);
                $(btn).children("b").text(data.likeStatus==1?'已赞':"赞");
            } else {
                alert(data.msg);
            }
        }
    );
}