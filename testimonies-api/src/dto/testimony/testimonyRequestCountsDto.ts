export class TestimonyRequestCountsDto {
  executionFactIds: string[];
  
  constructor(data: Required<TestimonyRequestCountsDto>) {
    this.executionFactIds = data.executionFactIds;
  }
}
