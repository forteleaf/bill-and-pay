/**
 * 포맷팅 유틸리티 함수 모음
 */

// Korean Bank Codes (settlementAccount.ts에서 이동)
export const KOREAN_BANK_CODES: Record<string, string> = {
	'004': 'KB국민은행',
	'011': 'NH농협은행',
	'020': '우리은행',
	'088': '신한은행',
	'081': '하나은행',
	'071': '우체국',
	'030': '대구은행',
	'031': '부산은행',
	'032': '광주은행',
	'034': '전북은행',
	'035': '제주은행',
	'039': '경남은행',
	'050': '저축은행',
	'089': '카카오뱅크',
	'090': '토스뱅크',
	'027': '씨티은행',
	'037': '전주은행',
	'040': '경주은행',
	'045': '새마을금고',
	'048': '신협',
	'060': '기업은행',
	'062': '광주신용금고',
	'064': '산림조합',
	'065': '해양수산신협',
	'066': '수협',
	'067': '신용보증기금',
	'068': '기술보증기금',
	'069': '한국주택금융공사',
	'070': '우리금융투자',
	'072': '한국투자증권',
	'073': '삼성증권',
	'074': '대우증권',
	'075': '미래에셋증권',
	'076': '메리츠증권',
	'077': '키움증권',
	'078': '이베스트투자증권',
	'079': '하이투자증권',
	'080': '하나금융투자',
	'082': '신한금융투자',
	'083': '현대차증권',
	'084': '투자',
	'085': '삼성금융투자',
	'086': '한국금융투자',
	'087': '한국증권금융',
	'091': '카카오페이증권',
	'092': '토스증권'
};

/**
 * 은행 코드를 은행명으로 변환
 */
export function getBankName(bankCode: string): string {
	return KOREAN_BANK_CODES[bankCode] || bankCode;
}

/**
 * 사업자번호 포맷팅 (xxx-xx-xxxxx)
 */
export function formatBusinessNumber(num?: string): string {
	if (!num) return '-';
	const digits = num.replace(/\D/g, '');
	if (digits.length !== 10) return num;
	return `${digits.slice(0, 3)}-${digits.slice(3, 5)}-${digits.slice(5)}`;
}

/**
 * 사업자번호 입력 실시간 포맷팅 (부분 입력 지원)
 */
export function formatBusinessNumberInput(value: string): string {
	const digits = value.replace(/\D/g, '').slice(0, 10);
	if (digits.length <= 3) return digits;
	if (digits.length <= 5) return `${digits.slice(0, 3)}-${digits.slice(3)}`;
	return `${digits.slice(0, 3)}-${digits.slice(3, 5)}-${digits.slice(5)}`;
}

/**
 * 계좌번호 마스킹 (끝 4자리만 표시)
 */
export function maskAccountNumber(accountNumber: string): string {
	if (!accountNumber || accountNumber.length < 4) {
		return accountNumber;
	}
	const lastFour = accountNumber.slice(-4);
	const masked = '*'.repeat(accountNumber.length - 4);
	return masked + lastFour;
}
