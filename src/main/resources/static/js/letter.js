$(function(){
	// 私信的发送按钮点击事件
	$("#sendBtn").click(send_letter);
	// 私信的关闭页面
	$(".close").click(delete_msg);
});

function send_letter() {
	// 发送之后隐藏发送的面板
	$("#sendModal").modal("hide");
	// 获取发送目标的名字，和发送的内容
	var toName = $("#recipient-name").val();
	var content = $("#message-text").val();
	$.post(
		// 地址
		CONTEXT_PATH + "/letter/send",
		// toName, content和方法的参数一致
		{"toName":toName,"content":content},
		// 返回值Json：data
		function(data) {
			// 解析JSON数据
			data = $.parseJSON(data);
			if(data.code == 0) {
				// 提示框的内容
				$("#hintBody").text("发送成功!");
			} else {
				$("#hintBody").text(data.msg);
			}
			// 设置提示框显示
			$("#hintModal").modal("show");
			// 设置提示框2秒后关闭
			setTimeout(function(){
				$("#hintModal").modal("hide");
				location.reload();
			}, 2000);
		}
	);
}

function delete_msg() {
	// TODO 删除数据
	$(this).parents(".media").remove();
}