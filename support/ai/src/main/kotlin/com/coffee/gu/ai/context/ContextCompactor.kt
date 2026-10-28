package com.coffee.gu.ai.context

import com.coffee.gu.ai.client.LlmMessage

class ContextCompactor(
    private val maxRawToolResultLength: Int = 200,
    private val keepRecentToolResultsCount: Int = 1
) {
    fun compact(messages: List<LlmMessage>): List<LlmMessage> {
        // =========================================================================
        // [Mission 3: 컨텍스트 엔지니어링 (Observation Masking) 구현하기]
        //
//         배경:
//         루프가 여러 번 돌면 과거에 실행했던 도구 결과(Role.TOOL)의 거대한 JSON들이
//         messages 리스트에 계속 남아 토큰을 낭비합니다.
//
//         요구사항:
//         1) messages 중에서 role == LlmMessage.Role.TOOL 인 메시지들을 찾습니다.
//         2) 가장 최근 keepRecentToolResultsCount(기본 1개)개의 TOOL 메시지는 원본을 그대로 유지해야 합니다.
//            (방금 실행한 도구 결과는 LLM이 읽어야 하므로!)
//         3) 그 이전의 '오래된 TOOL 메시지'들 중에서, content의 길이가 maxRawToolResultLength(200자)를 초과하는 메시지는
//            msg.copy(content = "[과거 도구('${msg.toolName}') 결과 압축됨]") 처럼 짧은 문자열로 치환한 새 리스트를 반환하세요!
//            (테스트 통과 조건: 치환된 문자열에 "[과거 도구('${msg.toolName}') 결과 압축됨" 문구가 포함되고 길이가 150자 미만이면 통과합니다)

        // * 아직 Mission 1, 2를 먼저 푸는 중이라면 일단 아래처럼 원본 messages를 그대로 return 해두셔도 됩니다!
        // =========================================================================
        val toolIndices = messages.indices.filter {messages[it].role == LlmMessage.Role.TOOL}
        val oldToolIndices = toolIndices.dropLast(keepRecentToolResultsCount)
        return messages.mapIndexed { index, msg ->
            if (oldToolIndices.contains(index) && (msg.content?.length ?: 0) > maxRawToolResultLength) {
                msg.copy(content = "[과거 도구('${msg.toolName}') 결과 압축됨]")
            } else {
                msg // 압축 대상이 아닌 메시지는 원본 그대로 새 리스트에 넣기!
            }
        }
    }
}
